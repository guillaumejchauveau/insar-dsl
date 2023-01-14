package io.basicbich.oui.compiler;

import io.basicbich.oui.compiler.exception.CompilationException;
import io.basicbich.oui.compiler.exception.CompilerException;

@FunctionalInterface
public interface Compiler<T, R> {
    R compile(T o) throws CompilerException, CompilationException;
}
