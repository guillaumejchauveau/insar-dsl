package io.basicbich.oui.tests;

import com.google.inject.Inject;
import io.basicbich.oui.oui.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(OuiInjectorProvider.class)
class OuiParsingTest {
    @Inject
    ParseHelper<Program> parseHelper;

    private String formatParserDiagnostics(Collection<Resource.Diagnostic> diagnostics) {
        return diagnostics.stream()
                .map(Resource.Diagnostic::getMessage)
                .collect(Collectors.joining("\n", "Parser diagnostics:\n", "\n"));
    }

    private String formatValidatorIssues(Collection<Issue> issues) {
        return issues.stream()
                .map(issue -> String.format("%s: %s", issue.getSeverity(), issue.getMessage()))
                .collect(Collectors.joining("\n", "Validator issues:\n", "\n"));
    }

    private Program parse(String text) throws Exception {
        var result = parseHelper.parse(text);
        assertNotNull(result);
        var errors = result.eResource().getErrors();
        assertTrue(errors.isEmpty(), formatParserDiagnostics(errors));
        return result;
    }

    private void validate(EObject object) {
        var validator = ((XtextResource) object.eResource()).getResourceServiceProvider().getResourceValidator();
        var errors = validator.validate(object.eResource(), CheckMode.ALL, CancelIndicator.NullImpl);
        assertTrue(errors.isEmpty(), formatValidatorIssues(errors));
    }

    @SuppressWarnings("unchecked")
    private <T extends EObject, R extends T> R assertSingleCollectionElement(EList<T> collection, Integer total, Integer target, Class<R> clazz) {
        assertEquals(total, collection.size());
        var element = collection.get(target);
        assertInstanceOf(clazz, element);
        return (R) element;
    }

    private <T extends Instruction> T assertSingleInstruction(String text, Integer total, Integer target, Class<T> clazz) throws Exception {
        var program = parse(text);
        validate(program);
        var instructions = program.getInstructions();
        assertNotNull(instructions);
        return assertSingleCollectionElement(instructions.getInstructions(), total, target, clazz);
    }

    @Test
    void emptyProgram() throws Exception {
        var result = parse("");
        validate(result);
        assertNull(result.getInstructions());
    }

    @Test
    void selectorScope() throws Exception {
        assertInstanceOf(RootSelectorScope.class, assertSingleInstruction("$", 1, 0, Selector.class).getScope());

        var filterSelector = assertSingleInstruction("filter", 1, 0, Selector.class);
        assertInstanceOf(FilterSelectorScope.class, filterSelector.getScope());
        assertEquals("filter", ((FilterSelectorScope) filterSelector.getScope()).getFilter().getName());

        var assignmentSelector = assertSingleInstruction("var=$ | $var", 2, 1, Selector.class);
        assertInstanceOf(AssignmentSelectorScope.class, assignmentSelector.getScope());
        assertEquals("var", ((AssignmentSelectorScope) assignmentSelector.getScope()).getAssignment().getName());

        var subScopeAssignment = parse("(var=$ | $var)");
        assertDoesNotThrow(() -> validate(subScopeAssignment));
        var subScopeUseAssignment = parse("var=$ | ($var)");
        assertDoesNotThrow(() -> validate(subScopeUseAssignment));

        var undeclaredAssignment = parse("$var");
        assertThrows(AssertionError.class, () -> validate(undeclaredAssignment));
        // TODO
        var outOfScopeAssignment = parse("(var=$) | $var");
        //assertThrows(AssertionError.class, () -> validate(outOfScopeAssignment));
        var outOfOrderAssignment = parse("$var | var=$");
        //assertThrows(AssertionError.class, () -> validate(outOfOrderAssignment));
    }

    @Test
    void attributeSelectorFragment() throws Exception {
        var attributeSelector = assertSingleInstruction("$.name", 1, 0, Selector.class);
        assertNotNull(attributeSelector.getFragments());
        var attributeFragment = assertSingleCollectionElement(attributeSelector.getFragments(), 1, 0, AttributeSelectorFragment.class);
        assertEquals("name", attributeFragment.getAttribute());
    }

    @Test
    void indexSelectorFragment() throws Exception {
        var empty = assertSingleInstruction("$[]", 1, 0, Selector.class);
        assertNotNull(empty.getFragments());
        var emptyFragment = assertSingleCollectionElement(empty.getFragments(), 1, 0, IndexSelectorFragment.class);
        assertTrue(emptyFragment.getIndexes().isEmpty());

        var single = assertSingleInstruction("$[1]", 1, 0, Selector.class);
        assertNotNull(single.getFragments());
        var singleFragment = assertSingleCollectionElement(single.getFragments(), 1, 0, IndexSelectorFragment.class);
        assertEquals(1, singleFragment.getIndexes().size());
        assertEquals(1, singleFragment.getIndexes().get(0));

        var multiple = assertSingleInstruction("$[2,6]", 1, 0, Selector.class);
        assertNotNull(multiple.getFragments());
        var multipleFragment = assertSingleCollectionElement(multiple.getFragments(), 1, 0, IndexSelectorFragment.class);
        assertArrayEquals(new Integer[]{2, 6}, multipleFragment.getIndexes().toArray());
    }

    @Test
    void sliceSelectorFragment() throws Exception {
        var sliceSelector = assertSingleInstruction("$[1:2]", 1, 0, Selector.class);
        assertNotNull(sliceSelector.getFragments());
        var sliceRangeFragment = assertSingleCollectionElement(sliceSelector.getFragments(), 1, 0, SliceSelectorFragment.class);
        assertEquals(1, sliceRangeFragment.getStart());
        assertEquals(2, sliceRangeFragment.getEnd());

        var startSliceSelector = assertSingleInstruction("$[5:]", 1, 0, Selector.class);
        assertNotNull(startSliceSelector.getFragments());
        var startSliceRangeFragment = assertSingleCollectionElement(startSliceSelector.getFragments(), 1, 0, StartSliceSelectorFragment.class);
        assertEquals(5, startSliceRangeFragment.getStart());
    }

    @Test
    void objectConstructor() throws Exception {
        var classicConstructor = assertSingleInstruction("{attribute: $}", 1, 0, ObjectConstructor.class);
        assertNotNull(classicConstructor.getAttributes());
        var attribute = assertSingleCollectionElement(classicConstructor.getAttributes(), 1, 0, ObjectAttribute.class);
        assertEquals("attribute", attribute.getName());

        var shortcutConstructor = assertSingleInstruction("var=$ | {$var}", 2, 1, ObjectConstructor.class);
        assertNotNull(shortcutConstructor.getAttributes());
        var shortcut = assertSingleCollectionElement(shortcutConstructor.getAttributes(), 1, 0, ObjectAttribute.class);
        assertNull(shortcut.getName());

        assertThrows(AssertionError.class, () -> validate(parse("{$}")));
        assertThrows(AssertionError.class, () -> validate(parse("{$[]}")));
    }

    @Test
    void assignment() throws Exception {
        var assignment = assertSingleInstruction("var=$", 1, 0, Assignment.class);
        assertEquals("var", assignment.getName());
    }
}
