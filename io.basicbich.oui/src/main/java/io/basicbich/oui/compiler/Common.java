package io.basicbich.oui.compiler;

import io.basicbich.oui.compiler.exception.CompilerException;
import io.basicbich.oui.compiler.exception.NonInferableObjectPropertyKeyException;
import io.basicbich.oui.oui.*;

import java.lang.Object;
import java.util.Objects;

public class Common {
    public static final class ParsedObjectProperty {
        private final String key;
        private final Instruction value;

        ParsedObjectProperty(String key, Instruction value) {
            this.key = key;
            this.value = value;
        }

        public String key() {
            return key;
        }

        public Instruction value() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ParsedObjectProperty) obj;
            return Objects.equals(this.key, that.key) &&
                    Objects.equals(this.value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "ParsedObjectProperty[" +
                    "key=" + key + ", " +
                    "value=" + value + ']';
        }
    }

    public static ParsedObjectProperty prepare(ObjectProperty property) {
        if (property.getKey() == null) {
            try {
                if (!(property.getValue() instanceof Selector selector)) {
                    throw new CompilerException("");
                }
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

                return new ParsedObjectProperty(key, property.getValue());
            } catch (CompilerException e) {
                throw new NonInferableObjectPropertyKeyException(property);
            }
        }
        return new ParsedObjectProperty(property.getKey(), property.getValue());
    }
}
