package io.basicbich.oui.tests.compiler;

@FunctionalInterface
public interface Compiler<T, R> {
	public R compile(T o);
}
