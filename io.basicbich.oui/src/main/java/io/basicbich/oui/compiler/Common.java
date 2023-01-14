package io.basicbich.oui.compiler;

import io.basicbich.oui.compiler.exception.CompilerException;
import io.basicbich.oui.compiler.exception.NonInferableObjectPropertyKeyException;
import io.basicbich.oui.oui.*;
import io.basicbich.oui.oui.impl.ObjectPropertyImpl;

public class Common {
    public static ObjectProperty prepare(ObjectProperty attr) {
        var attribute = attr;
        if (attribute.getKey() == null) {
            try {
                if (!(attribute.getValue() instanceof Selector)) {
                    throw new CompilerException("");
                }
                var selector = (Selector) attribute.getValue();
                String key;
                if (selector.getFragments().isEmpty()) {
                    key = (String) new AlternativeMapper<>()
                            .map(DeclarationSelectorScope.class, scope -> scope.getDeclaration().getName())
                            .map(FilterSelectorScope.class, scope -> scope.getFilter().getName())
                            .compile(selector.getScope());
                } else {
                    var fragments = selector.getFragments();
                    if (fragments.isEmpty()) {
                        throw new CompilerException("");
                    }
                    var lastFragment = fragments.get(fragments.size() - 1);
                    key = (String) new AlternativeMapper<>()
                            .map(PropertySelectorFragment.class, PropertySelectorFragment::getKey)
                            .compile(lastFragment);
                }
                attribute = new ObjectPropertyImpl() {
                };
                attribute.setKey(key);
                attribute.setValue(attr.getValue());
            } catch (CompilerException e) {
                throw new NonInferableObjectPropertyKeyException(attr);
            }
        }
        return attribute;
    }
}
