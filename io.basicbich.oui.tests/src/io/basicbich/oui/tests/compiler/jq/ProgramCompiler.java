package io.basicbich.oui.tests.compiler.jq;


import java.util.stream.Collectors;

import io.basicbich.oui.oui.Column;
import io.basicbich.oui.oui.Instruction;
import io.basicbich.oui.oui.NullableINT;
import io.basicbich.oui.oui.Program;
import io.basicbich.oui.oui.Selector;
import io.basicbich.oui.oui.SelectorFragment;
import io.basicbich.oui.oui.SelectorRangeFragment;
import io.basicbich.oui.oui.impl.SelectorAttributeFragmentImpl;
import io.basicbich.oui.oui.impl.SelectorImpl;
import io.basicbich.oui.oui.impl.SelectorRangeFragmentImpl;
import io.basicbich.oui.tests.compiler.AlternativeMapper;
import io.basicbich.oui.tests.compiler.Compiler;

public class ProgramCompiler implements Compiler<Program, String> {
	private String nInt(NullableINT nInt) {
		return nInt == null ? "" : Integer.toString(nInt.getVal());
	}
	
	private String selectorRangeFragment(SelectorRangeFragment selectorRangeFragment) {
		if (selectorRangeFragment.getStart() == null && selectorRangeFragment.getEnd() == null) {
			return ".[]";
		}
		return ".[" + nInt(selectorRangeFragment.getStart()) + ":" + nInt(selectorRangeFragment.getEnd()) + "]";
	}
	
	private String selectorFragment(SelectorFragment selectorFragment) {
		return (String) new AlternativeMapper<>()
				.map(SelectorRangeFragmentImpl.class, this::selectorRangeFragment)
				.map(SelectorAttributeFragmentImpl.class, attr -> "." + attr.getAttribute())
				.compile(selectorFragment);
	}
	
	private String selector(Selector selector) {
		var r = selector.getFragments().stream()
				.map(this::selectorFragment)
				.collect(Collectors.joining());
		return r.isEmpty() ? "." : r;
	}
	
	private String instruction(Instruction instruction) {
		return (String) new AlternativeMapper<>()
				.map(SelectorImpl.class, this::selector)
				.compile(instruction);
	}
	
	public String compile(Program program) {
		var r = program.getInstructions().stream()
				.map(this::instruction)
				.collect(Collectors.joining(" | "));
		
		return r + program.getOutput().getColumns().stream()
				.map(Column::getSelector)
				.map(this::selector)
				.collect(Collectors.joining(", ", " | [", "] | @csv"));
	}
}
