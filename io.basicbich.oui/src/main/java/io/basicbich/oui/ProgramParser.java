package io.basicbich.oui;

import io.basicbich.oui.oui.Program;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProgramParser {

    public ProgramParser() {
        OuiStandaloneSetup.doSetup();
    }

    public Program parse(URI uri) {
        var resource = new ResourceSetImpl().getResource(uri, true);
        var program = (Program) resource.getContents().get(0);

        var parsingErrors = program.eResource().getErrors();
        if (!parsingErrors.isEmpty()) {
            var errors = parsingErrors.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            throw new RuntimeException("Failed to parse program:\n" + errors);
        }

        var validator = ((XtextResource) program.eResource()).getResourceServiceProvider().getResourceValidator();

        var validationErrors = validator.validate(program.eResource(), CheckMode.ALL, CancelIndicator.NullImpl);
        if (!validationErrors.isEmpty()) {
            var errors = validationErrors.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            throw new RuntimeException("Validation errors:\n" + errors);
        }
        return program;
    }

    public Program parse(Path path) {
        return parse(URI.createFileURI(path.toString()));
    }

    public Program parse(String text) {
        try {
            var temp = File.createTempFile(UUID.randomUUID().toString(), ".oui");
            var bw = new BufferedWriter(new FileWriter(temp));
            bw.write(text);
            bw.close();

            var program = parse(temp.toPath());

            temp.delete();
            return program;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file", e);
        }
    }
}
