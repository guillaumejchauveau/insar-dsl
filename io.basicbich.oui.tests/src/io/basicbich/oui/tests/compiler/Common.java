package io.basicbich.oui.tests.compiler;

import io.basicbich.oui.oui.AssignmentSelectorScope;
import io.basicbich.oui.oui.AttributeSelectorFragment;
import io.basicbich.oui.oui.FilterSelectorScope;
import io.basicbich.oui.oui.ObjectAttribute;
import io.basicbich.oui.oui.impl.ObjectAttributeImpl;
import io.basicbich.oui.tests.compiler.exception.CompilerException;
import io.basicbich.oui.tests.compiler.exception.NonInferableAttributeNameException;

public class Common {
    public static ObjectAttribute prepare(ObjectAttribute attr) {
        var attribute = attr;
        if (attribute.getName() == null) {
            try {
                String name;
                if (attribute.getSelector().getFragments().isEmpty()) {
                    name = (String) new AlternativeMapper<>()
                            .map(AssignmentSelectorScope.class, scope -> scope.getAssignment().getName())
                            .map(FilterSelectorScope.class, scope -> scope.getFilter().getName())
                            .compile(attribute.getSelector().getScope());
                } else {
                    var fragments = attribute.getSelector().getFragments();
                    if (fragments.isEmpty()) {
                        throw new NonInferableAttributeNameException(attr);
                    }
                    var lastFragment = fragments.get(fragments.size() - 1);
                    name = (String) new AlternativeMapper<>()
                            .map(AttributeSelectorFragment.class, AttributeSelectorFragment::getAttribute)
                            .compile(lastFragment);
                }
                attribute = new ObjectAttributeImpl() {
                };
                attribute.setName(name);
                attribute.setSelector(attr.getSelector());
            } catch (CompilerException e) {
                throw new NonInferableAttributeNameException(attr);
            }
        }
        return attribute;
    }
}
