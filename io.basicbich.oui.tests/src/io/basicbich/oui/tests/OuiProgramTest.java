package io.basicbich.oui.tests;

import io.basicbich.oui.tests.compiler.jq.ProgramCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OuiProgramTest {
    ProgramParser parser = new ProgramParser();
    ProgramCompiler jqCompiler = new ProgramCompiler();

    private void assertValidJQProgram(String input, String expectedOutput) {
        var program = parser.parse(input);
        Assertions.assertEquals(expectedOutput, jqCompiler.compile(program));
    }

    @Test
    void a() {
        var input = "";
        var expectedOutput = ProgramCompiler.PROGRAM_EPILOG;
        assertValidJQProgram(input, expectedOutput);
    }

    @Test
    void c() {
        var input = "$.quiz | entries | category=$.key | $.value | entries | {id: $.key, $category, $.value.question}";
        var expectedOutput = "[.quiz | to_entries[] | .key as $category | .value | to_entries[] | {id: .key, category: $category, question: .value.question}] | " + ProgramCompiler.PROGRAM_EPILOG;
        assertValidJQProgram(input, expectedOutput);
    }

    @Test
    void d() {
        var input = "{ color: $.colors[:], car: $.cars[:] }";
        var expectedOutput = "[{color: .colors[], car: .cars[]}] | " + ProgramCompiler.PROGRAM_EPILOG;
        assertValidJQProgram(input, expectedOutput);
    }

    @Test
    void e() {
        var input = "$[:] | port=$.ports[:] | { $.ip, $port.port, $port.proto }";
        var expectedOutput = "[.[] | .ports[] as $port | {ip: .ip, port: $port.port, proto: $port.proto}] | " + ProgramCompiler.PROGRAM_EPILOG;
        assertValidJQProgram(input, expectedOutput);
    }

    @Test
    void f() {
        var input = "$[:][:]";
        var expectedOutput = "[.[][]] | " + ProgramCompiler.PROGRAM_EPILOG;
        assertValidJQProgram(input, expectedOutput);
    }
}
