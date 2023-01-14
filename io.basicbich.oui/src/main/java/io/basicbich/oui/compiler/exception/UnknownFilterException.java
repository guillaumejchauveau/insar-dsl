package io.basicbich.oui.compiler.exception;

import io.basicbich.oui.oui.Filter;

public class UnknownFilterException extends CompilationException {
    public UnknownFilterException(Filter filter) {
        super(filter, "Unknown filter: " + filter.getName());
    }
}
