package io.basicbich.oui.compiler.exception;

import io.basicbich.oui.oui.ObjectAttribute;

public class NonInferableAttributeNameException extends CompilationException {
    public NonInferableAttributeNameException(ObjectAttribute source) {
        super(source, "Attribute name is not specified and cannot be inferred from selector");
    }
}
