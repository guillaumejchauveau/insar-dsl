package io.basicbich.oui.compiler;

import io.basicbich.oui.compiler.exception.CompilerException;

import java.util.HashMap;
import java.util.Map;

public class AlternativeMapper<Base, R> implements Compiler<Base, R> {
    private final Map<Class<? extends Base>, Compiler<? extends Base, R>> map = new HashMap<>();

    public <T extends Base> AlternativeMapper<Base, R> map(Class<T> alternativeType, Compiler<T, R> alternativeCompiler) {
        map.put(alternativeType, alternativeCompiler);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R compile(Base o) {
        var compiler = (Compiler<Base, R>) map.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(o))
                .findFirst()
                .orElseThrow(() -> new CompilerException("No alternative found for " + o.getClass()))
                .getValue();
        return compiler.compile(o);
    }
}
