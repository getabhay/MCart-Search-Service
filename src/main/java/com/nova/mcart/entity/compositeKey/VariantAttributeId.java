package com.nova.mcart.entity.compositeKey;

import java.io.Serializable;
import java.util.Objects;

public class VariantAttributeId implements Serializable {

    private Long variant;
    private Long attribute;

    public VariantAttributeId() {}

    public VariantAttributeId(Long variant, Long attribute) {
        this.variant = variant;
        this.attribute = attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariantAttributeId that)) return false;
        return Objects.equals(variant, that.variant)
                && Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, attribute);
    }
}
