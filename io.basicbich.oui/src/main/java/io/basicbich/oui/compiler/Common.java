package io.basicbich.oui.compiler;

import io.basicbich.oui.compiler.exception.CompilerException;
import io.basicbich.oui.compiler.exception.NonInferableAttributeNameException;
import io.basicbich.oui.oui.*;
import io.basicbich.oui.oui.impl.ObjectAttributeImpl;

public class Common {
    public static ObjectAttribute prepare(ObjectAttribute attr) {
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
                            .map(AttributeSelectorFragment.class, AttributeSelectorFragment::getKey)
                            .compile(lastFragment);
                }
                attribute = new ObjectAttributeImpl() {
                };
                attribute.setKey(key);
                attribute.setValue(attr.getValue());
            } catch (CompilerException e) {
                throw new NonInferableAttributeNameException(attr);
            }
        }
        return attribute;
    }
}
