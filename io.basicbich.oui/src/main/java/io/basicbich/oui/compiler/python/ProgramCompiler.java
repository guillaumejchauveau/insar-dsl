package io.basicbich.oui.compiler.python;

import io.basicbich.oui.compiler.AlternativeMapper;
import io.basicbich.oui.compiler.Common;
import io.basicbich.oui.compiler.Compiler;
import io.basicbich.oui.oui.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ProgramCompiler implements Compiler<Program, String> {
    public static final String PROGRAM_PROLOG = "prolog.py";
    public static final String PROGRAM_EPILOG = "epilog.py";

    private String indexSelectorFragment(IndexSelectorFragment fragment) {
        return fragment.getIndexes().stream()
                .map(java.lang.Object::toString)
                .collect(Collectors.joining(", ", "IndexSelectorFragment([", "])"));
    }

    private String selectorFragment(SelectorFragment fragment) {
        return (String) new AlternativeMapper<>()
                .map(PropertySelectorFragment.class, prop -> "PropertySelectorFragment(\"" + prop.getKey() + "\")")
                .map(IndexSelectorFragment.class, this::indexSelectorFragment)
                .map(SliceSelectorFragment.class, slice -> "SliceSelectorFragment(" + slice.getStart() + ", " + slice.getEnd() + ")")
                .map(StartSliceSelectorFragment.class, slice -> "SliceSelectorFragment(" + slice.getStart() + ", -1)")
                .compile(fragment);
    }


    private String selector(Selector selector) {
        var fragments = selector.getFragments().stream().map(this::selectorFragment)
                .collect(Collectors.joining(", "));
        var scope = new AlternativeMapper<>()
                .map(RootSelectorScope.class, s -> "")
                .map(DeclarationSelectorScope.class, s -> ", " + "\"" + s.getDeclaration().getName() + "\"")
                .map(FilterSelectorScope.class, s -> ", " + s.getFilter().getName())
                .compile(selector.getScope());
        return "Selector([" + fragments + "]" + scope + ")";
    }

    private String object(io.basicbich.oui.oui.Object object) {
        return object.getProperties().stream()
                .map(Common::prepare)
                .map(prop -> '"' + prop.getKey() + '"' + ": " + this.instruction(prop.getValue()))
                .collect(Collectors.joining(", ", "Object({", "})"));
    }

    private String instruction(Instruction instruction) {
        return (String) new AlternativeMapper<>()
                .map(Selector.class, this::selector)
                .map(io.basicbich.oui.oui.Object.class, this::object)
                .map(InstructionSet.class, this::instructionSet)
                .compile(instruction);
    }

    private String instructionSet(InstructionSet instructionSet) {
        var declarations = instructionSet.getDeclarations().stream()
                .map(dec -> "\"" + dec.getName() + "\": " + this.instruction(dec.getValue()))
                .collect(Collectors.joining(", "));

        var result = "InstructionSet({" + declarations + "}, " + this.instruction(instructionSet.getInstruction());
        if (instructionSet.getNext() != null) {
            result += ", " + this.instructionSet(instructionSet.getNext());
        }
        return result + ")";
    }

    @Override
    public String compile(Program o) {
        var prolog = this.readFile(PROGRAM_PROLOG);
        var prog = "";
        var epilog = this.readFile(PROGRAM_EPILOG);

        if (o.getFirst() != null) {
            prog = "prog = " + this.instructionSet(o.getFirst()) + "\n";
            prog += "data = prog(data)\n";
        }

        return prolog + prog + epilog;
    }

    private String readFile(String fileName) {
        var fileInputStream = ClassLoader.getSystemResourceAsStream(fileName);
        if (fileInputStream == null) {
            throw new RuntimeException("Cannot find " + fileName);
        }
        var bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        var result = bufferedReader.lines().collect(Collectors.joining("\n")) + "\n";
        try {
            fileInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
