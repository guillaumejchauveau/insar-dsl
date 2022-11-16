package io.basicbich.oui.tests.compiler;

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
        return ((Compiler<Base, R>) map.get(o.getClass())).compile(o);
    }
}
