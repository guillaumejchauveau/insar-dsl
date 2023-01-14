package io.basicbich.oui.compiler.jq;


import io.basicbich.oui.compiler.AlternativeMapper;
import io.basicbich.oui.compiler.Common;
import io.basicbich.oui.compiler.Compiler;
import io.basicbich.oui.compiler.exception.UnknownFilterException;
import io.basicbich.oui.oui.*;

import java.util.stream.Collectors;

public class ProgramCompiler implements Compiler<Program, String> {
    public static final String PROGRAM_EPILOG = "(first(.[]) | keys_unsorted), (.[] | [.[]]) | @csv";

    private String indexSelectorFragment(IndexSelectorFragment fragment) {
        return fragment.getIndexes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String selectorFragment(SelectorFragment fragment) {
        return (String) new AlternativeMapper<>()
                .map(AttributeSelectorFragment.class, attr -> "." + attr.getKey())
                .map(IndexSelectorFragment.class, this::indexSelectorFragment)
                .map(SliceSelectorFragment.class, range -> "[" + range.getStart() + ":" + range.getEnd() + "]")
                .map(StartSliceSelectorFragment.class, range -> "[" + range.getStart() + ":]")
                .compile(fragment);
    }

    private String filter(Filter filter) {
        switch (filter.getName()) {
            case "entries":
                return "to_entries[]";
            case "keys":
                return "keys_unsorted[]";
            case "values":
                return "values[]";
            case "length":
                return "length";
            default:
                throw new UnknownFilterException(filter);
        }
    }

    private String selector(Selector selector) {
        var fragments = selector.getFragments().stream()
                .map(this::selectorFragment)
                .collect(Collectors.joining());
        return new AlternativeMapper<>()
                .map(RootSelectorScope.class, scope -> fragments.startsWith(".") ? "" : ".")
                .map(DeclarationSelectorScope.class, scope -> "$" + scope.getDeclaration().getName())
                .map(FilterSelectorScope.class, scope -> this.filter(scope.getFilter()))
                .compile(selector.getScope())
                + fragments;
    }

    private String objectConstructor(ObjectConstructor objectConstructor) {
        return objectConstructor.getAttributes().stream()
                .map(Common::prepare)
                .map(attr -> attr.getKey() + ": " + this.instruction(attr.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String instruction(Instruction instruction) {
        return (String) new AlternativeMapper<>()
                .map(Selector.class, this::selector)
                .map(ObjectConstructor.class, this::objectConstructor)
                .map(InstructionSet.class, set -> "(" + this.instructionSet(set) + ")")
                .compile(instruction);
    }

    private String instructionSet(InstructionSet instructionSet) {
        var result = instructionSet.getDeclarations().stream()
                .map(dec -> this.instruction(dec.getValue()) + " as $" + dec.getName())
                .collect(Collectors.joining(" | "));
        if (!result.isEmpty()) {
            result += " | ";
        }
        result += this.instruction(instructionSet.getInstruction());

        if (instructionSet.getNext() != null) {
            result += " | " + this.instructionSet(instructionSet.getNext());
        }
        return result;
    }

    public String compile(Program program) {
        if (program.getFirst() == null) {
            return PROGRAM_EPILOG;
        }
        return "[" + this.instructionSet(program.getFirst()) + "] | " + PROGRAM_EPILOG;
    }
}
