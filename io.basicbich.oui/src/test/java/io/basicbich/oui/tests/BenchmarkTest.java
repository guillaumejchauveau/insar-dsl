package io.basicbich.oui.tests;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.basicbich.oui.ProgramParser;
import io.basicbich.oui.compiler.jq.ProgramCompiler;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BenchmarkTest {
    static class PerformanceResult {
        final String name;
        final String variant;
        final Integer max_resident_set_size;
        final Float elapsed_wall_time;

        PerformanceResult(String name, String variant, Integer max_resident_set_size, Float elapsed_wall_time) {
            this.name = name;
            this.variant = variant;
            this.max_resident_set_size = max_resident_set_size;
            this.elapsed_wall_time = elapsed_wall_time;
        }
    }

    protected static final Collection<PerformanceResult> performanceResults = Collections.synchronizedCollection(new ArrayList<>());

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static class Test {
        final ProgramParser parser = new ProgramParser();
        final ProgramCompiler jqCompiler = new ProgramCompiler();

        final Path ouiFile;
        final Optional<Path> jsonFile;
        final Optional<Path> jqFile;
        final Optional<Path> csvFile;

        Test(List<Path> files) {
            ouiFile = files.stream().filter(filterExtension(OUI_EXTENSION)).findFirst().orElseThrow();
            jsonFile = files.stream().filter(filterExtension(JSON_EXTENSION)).findFirst();
            jqFile = files.stream().filter(filterExtension(JQ_EXTENSION)).findFirst();
            csvFile = files.stream().filter(filterExtension(CSV_EXTENSION)).findFirst();
        }

        void compile() throws Throwable {
            var program = parser.parse(ouiFile);
            var jqProgram = jqCompiler.compile(program);
            if (jqFile.isPresent()) {
                var jq = Files.readString(jqFile.get());
                Assertions.assertEquals(jq, jqProgram);
            }
            // TODO: compile to python
        }

        void run() throws Throwable {
            var jsonFile = this.jsonFile.orElseThrow().toFile();
            var program = parser.parse(ouiFile);
            var jqProgram = jqCompiler.compile(program);

            var jqCSV = this.readCSV(new InputStreamReader(this.runProcess(jsonFile, "jq", "-r", jqProgram)));

            // TODO: run python program

            if (csvFile.isPresent()) {
                var referenceCSVReader = new CSVReader(Files.newBufferedReader(this.csvFile.get()));
                var referenceCSV = referenceCSVReader.readAll();
                referenceCSVReader.close();

                Assertions.assertEquals(referenceCSV.size(), jqCSV.size());
                for (int i = 0; i < referenceCSV.size(); i++) {
                    Assertions.assertArrayEquals(referenceCSV.get(i), jqCSV.get(i));
                }
                // TODO: compare python output
            } else {
                // TODO: compare outputs to each other
            }
        }

        void evaluate() throws Throwable {
            var jsonFile = this.jsonFile.orElseThrow().toFile();
            var program = parser.parse(ouiFile);
            var jqProgram = jqCompiler.compile(program);
            // TODO: compile to python

            for (int i = 0; i < BenchmarkTest.BENCHMARK_SAMPLING; i++) {
                BenchmarkTest.performanceResults.add(this.evaluateProcess("jq", jsonFile, "jq", jqProgram));
                // TODO: evaluate python program
            }
        }

        private InputStream runProcess(File input, String... command) throws IOException, InterruptedException {
            var processBuilder = new ProcessBuilder(command);
            processBuilder.redirectInput(input);
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
    static final String JSON_EXTENSION = "json";
    static final String JQ_EXTENSION = "jq";
    static final String CSV_EXTENSION = "csv";

    private static Predicate<Path> filterExtension(String extension) {
        return path -> path.getFileName().toString().endsWith(extension);
    }

    @TestFactory
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
