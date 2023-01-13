package io.basicbich.oui.tests.compiler.exception;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

public class CompilationException extends RuntimeException {
    final EObject source;

    private static String formatMessage(EObject source, String message) {
        var node = NodeModelUtils.getNode(source);
        return String.format("Error at %d:%d, %s", node.getStartLine(), node.getOffset(), message);
    }

    public CompilationException(EObject source, String message) {
        super(formatMessage(source, message));
        this.source = source;
    }
}
