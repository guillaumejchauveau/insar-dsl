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
                .map(java.lang.Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String selectorFragment(SelectorFragment fragment) {
        return (String) new AlternativeMapper<>()
                .map(PropertySelectorFragment.class, prop -> "." + prop.getKey())
                .map(IndexSelectorFragment.class, this::indexSelectorFragment)
                .map(SliceSelectorFragment.class, slice -> "[" + slice.getStart() + ":" + slice.getEnd() + "]")
                .map(StartSliceSelectorFragment.class, slice -> "[" + slice.getStart() + ":]")
                .compile(fragment);
    }

    private String filter(Filter filter) {
        return switch (filter.getName()) {
            case "entries" -> "to_entries[]";
            case "keys" -> "keys_unsorted[]";
            case "values" -> "values[]";
            case "length" -> "length";
            default -> throw new UnknownFilterException(filter);
        };
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

    private String object(io.basicbich.oui.oui.Object object) {
        return object.getProperties().stream()
                .map(Common::prepare)
                .map(prop -> prop.getKey() + ": " + this.instruction(prop.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String instruction(Instruction instruction) {
        return (String) new AlternativeMapper<>()
                .map(Selector.class, this::selector)
                .map(io.basicbich.oui.oui.Object.class, this::object)
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
