package io.basicbich.oui.tests.compiler;

import io.basicbich.oui.tests.compiler.exception.CompilationException;
import io.basicbich.oui.tests.compiler.exception.CompilerException;

@FunctionalInterface
public interface Compiler<T, R> {
    R compile(T o) throws CompilerException, CompilationException;
}
