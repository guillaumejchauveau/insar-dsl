package io.basicbich.oui.tests;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.basicbich.oui.ProgramParser;
import io.basicbich.oui.compiler.Compiler;
import io.basicbich.oui.oui.Program;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BenchmarkTest {
    record PerformanceResult(String name, String variant, Integer max_resident_set_size, Float elapsed_wall_time) {
    }

    protected static final Collection<PerformanceResult> performanceResults = Collections.synchronizedCollection(new ArrayList<>());

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static class Test {
        final ProgramParser parser = new ProgramParser();
        final Compiler<Program, String> jqCompiler = new io.basicbich.oui.compiler.jq.ProgramCompiler();
        final Compiler<Program, String> pythonCompiler = new io.basicbich.oui.compiler.python.ProgramCompiler();

        final Path ouiFile;
        final Optional<Path> jsonSchemaFile;
        final Optional<Path> jsonFile;
        final Optional<Path> jqFile;
        final Optional<Path> pythonFile;
        final Optional<Path> csvFile;

        Test(List<Path> files) {
            ouiFile = files.stream().filter(filterExtension(OUI_EXTENSION)).findFirst().orElseThrow();
            jsonSchemaFile = files.stream().filter(filterExtension(JSON_SCHEMA_EXTENSION)).findFirst();
            jsonFile = files.stream().filter(filterExtension(JSON_EXTENSION)).findFirst();
            jqFile = files.stream().filter(filterExtension(JQ_EXTENSION)).findFirst();
            pythonFile = files.stream().filter(filterExtension(PYTHON_EXTENSION)).findFirst();
            csvFile = files.stream().filter(filterExtension(CSV_EXTENSION)).findFirst();
        }

        void compile() throws Throwable {
            var program = parser.parse(ouiFile);
            var jqProgram = jqCompiler.compile(program);
            if (jqFile.isPresent()) {
                var jq = Files.readString(jqFile.get());
                Assertions.assertEquals(jq, jqProgram);
            }
            var pythonProgram = pythonCompiler.compile(program);
            if (pythonFile.isPresent()) {
                var python = Files.readString(pythonFile.get());
                Assertions.assertEquals(python, pythonProgram);
            }
        }

        void run() throws Throwable {
            File jsonFile;
            if (this.jsonSchemaFile.isPresent()) {
                jsonFile = this.generateJsonFile(jsonSchemaFile.get(), 1);
            } else if (this.jsonFile.isPresent()) {
                jsonFile = this.jsonFile.get().toFile();
            } else {
                throw new IllegalStateException("No JSON file or JSON schema file found");
            }

            var program = parser.parse(ouiFile);
            var jqProgram = jqCompiler.compile(program);
            var pythonProgram = pythonCompiler.compile(program);

            var jqCSV = this.readCSV(new InputStreamReader(this.runProcess(jsonFile, "jq", "-r", jqProgram)));
            var pythonCSV = this.readCSV(new InputStreamReader(this.runProcess(jsonFile, "python3", "-c", pythonProgram)));

            if (csvFile.isPresent()) {
                var referenceCSVReader = new CSVReader(Files.newBufferedReader(this.csvFile.get()));
                var referenceCSV = referenceCSVReader.readAll();
                referenceCSVReader.close();

                Assertions.assertEquals(referenceCSV.size(), jqCSV.size());
                for (int i = 0; i < referenceCSV.size(); i++) {
                    Assertions.assertArrayEquals(referenceCSV.get(i), jqCSV.get(i));
                }

                Assertions.assertEquals(referenceCSV.size(), pythonCSV.size());
                for (int i = 0; i < referenceCSV.size(); i++) {
                    Assertions.assertArrayEquals(referenceCSV.get(i), pythonCSV.get(i));
                }
            } else {
                Assertions.assertEquals(jqCSV, pythonCSV);
            }
        }

        void evaluate() throws Throwable {
            File jsonFile;
            if (this.jsonSchemaFile.isPresent()) {
                jsonFile = this.generateJsonFile(jsonSchemaFile.get(), 1000);
            } else if (this.jsonFile.isPresent()) {
                jsonFile = this.jsonFile.get().toFile();
            } else {
                throw new IllegalStateException("No JSON file or JSON schema file found");
            }

            var program = parser.parse(ouiFile);
            var jqProgram = jqCompiler.compile(program);
            var pythonProgram = pythonCompiler.compile(program);

            for (int i = 0; i < BenchmarkTest.BENCHMARK_SAMPLING; i++) {
                BenchmarkTest.performanceResults.add(this.evaluateProcess("jq", jsonFile, "jq", "-r", jqProgram));
                BenchmarkTest.performanceResults.add(this.evaluateProcess("python", jsonFile, "python3", "-c", pythonProgram));
            }
        }

        private File generateJsonFile(Path schemaFile, Integer itemsCount) throws IOException, InterruptedException {
            var generatorConfigFile = File.createTempFile("json-schema-generator-options", ".js");
            generatorConfigFile.deleteOnExit();
            var writer = new FileWriter(generatorConfigFile);
            writer.write("module.exports = {\n");
            writer.write("  minItems: " + itemsCount + ",\n");
            writer.write("  maxItems: " + itemsCount + ",\n");
            writer.write("  minProperties: " + itemsCount + ",\n");
            writer.write("  maxProperties: " + itemsCount + "\n");
            writer.write("};\n");
            writer.close();

            var generatedJsonFile = File.createTempFile("json", ".json");
            generatedJsonFile.deleteOnExit();

            var processBuilder = new ProcessBuilder("generate-json", schemaFile.toString(), generatedJsonFile.toString(), "none", generatorConfigFile.getAbsolutePath());
            processBuilder.redirectOutput(generatedJsonFile);
            var process = processBuilder.start();
            if (process.waitFor() != 0) {
                throw new RuntimeException(new String(process.getErrorStream().readAllBytes()));
            }
            return generatedJsonFile;
        }

        private InputStream runProcess(File input, String... command) throws IOException, InterruptedException {
            var processBuilder = new ProcessBuilder(command);
            processBuilder.redirectInput(input);
            processBuilder.redirectOutput(new File("/dev/null"));
            var process = processBuilder.start();
            Assertions.assertEquals(0, process.waitFor());
            var stderr = new String(process.getErrorStream().readAllBytes());
            Assertions.assertTrue(stderr.isEmpty());
            return process.getInputStream();
        }

        private List<String[]> readCSV(InputStreamReader reader) throws IOException, CsvException {
            var csvReader = new CSVReader(reader);
            var csv = csvReader.readAll();
            csvReader.close();
            return csv;
        }

        private PerformanceResult evaluateProcess(String variant, File input, String... command) throws IOException, InterruptedException {
            var args = new ArrayList<String>();
            args.addAll(List.of("time", "-f", "'%M %e'", "--"));
            args.addAll(List.of(command));
            var processBuilder = new ProcessBuilder(args);
            processBuilder.redirectInput(input);
            processBuilder.redirectOutput(new File("/dev/null"));
            var process = processBuilder.start();
            Assertions.assertEquals(0, process.waitFor());
            var result = new String(process.getErrorStream().readAllBytes())
                    .trim()
                    .replaceAll("'", "")
                    .split(" ");

            return new PerformanceResult(ouiFile.getFileName().toString(), variant, Integer.valueOf(result[0]), Float.valueOf(result[1]));
        }

        DynamicContainer toDynamicContainer() {
            var tests = new ArrayList<DynamicTest>();
            tests.add(DynamicTest.dynamicTest("compile", this::compile));
            if (jsonFile.isPresent()) {
                tests.add(DynamicTest.dynamicTest("run", this::run));
                tests.add(DynamicTest.dynamicTest("evaluate", this::evaluate));
            }
            return DynamicContainer.dynamicContainer(ouiFile.getFileName().toString(), tests);
        }
    }

    static final Integer BENCHMARK_SAMPLING = 10;
    static final Path BENCHMARK_FOLDER_PATH = Paths.get("../benchmark");
    static final Path BENCHMARK_EVALUATION_RESULTS_PATH = Paths.get("../benchmark/evaluation-results.csv");
    static final String OUI_EXTENSION = "oui";
    static final String JSON_SCHEMA_EXTENSION = "schema.json";
    static final String JSON_EXTENSION = "json";
    static final String JQ_EXTENSION = "jq";
    static final String PYTHON_EXTENSION = "py";
    static final String CSV_EXTENSION = "csv";

    private static Predicate<Path> filterExtension(String extension) {
        return path -> path.getFileName().toString().endsWith(extension);
    }

    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    Stream<DynamicContainer> files() throws IOException {
        try (var stream = Files.list(BENCHMARK_FOLDER_PATH)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .collect(Collectors.groupingBy(file -> file.getFileName().toString().split("\\.", 2)[0]))
                    .values()
                    .stream()
                    .filter(paths -> paths.stream().anyMatch(filterExtension(OUI_EXTENSION)))
                    .map(Test::new)
                    .map(Test::toDynamicContainer);
        }
    }

    @AfterAll
    static void afterAll() throws IOException {
        var csv = new StringBuilder();
        csv.append("Name,Variant,Elapsed wall clock time (s),Maximum resident set size (kbytes)\n");
        synchronized (performanceResults) {
            for (var result : performanceResults) {
                csv.append(result.name).append(',');
                csv.append(result.variant).append(',');
                csv.append(result.elapsed_wall_time).append(',');
                csv.append(result.max_resident_set_size).append('\n');
            }
        }
        Files.writeString(BENCHMARK_EVALUATION_RESULTS_PATH, csv);
    }
}
