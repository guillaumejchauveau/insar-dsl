package io.basicbich.oui.compiler.exception;

import io.basicbich.oui.oui.ObjectProperty;

public class NonInferableObjectPropertyKeyException extends CompilationException {
    public NonInferableObjectPropertyKeyException(ObjectProperty source) {
        super(source, "Property key is not specified and cannot be inferred from instruction");
    }
}
