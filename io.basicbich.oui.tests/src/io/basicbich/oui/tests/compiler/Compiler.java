package io.basicbich.oui.tests.compiler;

@FunctionalInterface
public interface Compiler<T, R> {
    R compile(T o);
}
