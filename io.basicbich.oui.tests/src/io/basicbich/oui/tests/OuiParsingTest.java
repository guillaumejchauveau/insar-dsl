package io.basicbich.oui.tests;

import com.google.inject.Inject;
import io.basicbich.oui.oui.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.stream.Collectors;

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
        Assertions.assertNotNull(result);
        var errors = result.eResource().getErrors();
        Assertions.assertTrue(errors.isEmpty(), formatParserDiagnostics(errors));
        return result;
    }

    private void validate(Program program) {
        var validator = ((XtextResource) program.eResource()).getResourceServiceProvider().getResourceValidator();
        var errors = validator.validate(program.eResource(), CheckMode.ALL, CancelIndicator.NullImpl);
        Assertions.assertTrue(errors.isEmpty(), formatValidatorIssues(errors));
    }

    @Test
    void emptyProgram() throws Exception {
        var result = parse("");
        validate(result);
        Assertions.assertNull(result.getInstructions());
    }

    @Test
    void rootSelectorScope() throws Exception {
        var rootScope = parse("$");
        validate(rootScope);
        var rootInstructions = rootScope.getInstructions();
        Assertions.assertNotNull(rootInstructions);
        Assertions.assertEquals(1, rootInstructions.getInstructions().size());
        var rootInstruction = rootInstructions.getInstructions().get(0);
        Assertions.assertInstanceOf(Selector.class, rootInstruction);
        var rootSelector = (Selector) rootInstruction;
        Assertions.assertInstanceOf(RootSelectorScope.class, rootSelector.getScope());
    }

    @Test
    void filterSelectorScope() throws Exception {
        var filterScope = parse("filter");
        validate(filterScope);
        var filterInstructions = filterScope.getInstructions();
        Assertions.assertNotNull(filterInstructions);
        Assertions.assertEquals(1, filterInstructions.getInstructions().size());
        var filterInstruction = filterInstructions.getInstructions().get(0);
        Assertions.assertInstanceOf(Selector.class, filterInstruction);
        var filterSelector = (Selector) filterInstruction;
        Assertions.assertInstanceOf(FilterSelectorScope.class, filterSelector.getScope());
        Assertions.assertEquals("filter", ((FilterSelectorScope) filterSelector.getScope()).getFilter().getName());
    }

    @Test
    void assignmentSelectorScope() throws Exception {
        var assignmentScope = parse("var=$ | $var");
        validate(assignmentScope);
        var assignmentInstructions = assignmentScope.getInstructions();
        Assertions.assertNotNull(assignmentInstructions);
        Assertions.assertEquals(2, assignmentInstructions.getInstructions().size());
        var assignmentInstruction = assignmentInstructions.getInstructions().get(1);
        Assertions.assertInstanceOf(Selector.class, assignmentInstruction);
        var assignmentSelector = (Selector) assignmentInstruction;
        Assertions.assertInstanceOf(AssignmentSelectorScope.class, assignmentSelector.getScope());
        Assertions.assertEquals("var", ((AssignmentSelectorScope) assignmentSelector.getScope()).getAssignment().getName());
        var unknownAssignment = parse("$var");
        Assertions.assertThrows(AssertionError.class, () -> validate(unknownAssignment));
    }

    @Test
    void rangeSelectorFragment() throws Exception {
        // TODO
    }

    @Test
    void attributeSelectorFragment() throws Exception {
        // TODO
    }

    @Test
    void indexSelectorFragment() throws Exception {
        // TODO
    }

    @Test
    void objectConstructor() throws Exception {
        // TODO
    }

    @Test
    void assignment() throws Exception {
        // TODO
    }

    @Test
    void filter() throws Exception {
        // TODO
    }
}
