package com.nova.mcart.entity.compositeKey;

import java.io.Serializable;
import java.util.Objects;

public class ProductAttributeId implements Serializable {

    private Long product;
    private Long attribute;

    public ProductAttributeId() {}

    public ProductAttributeId(Long product, Long attribute) {
        this.product = product;
        this.attribute = attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductAttributeId that)) return false;
        return Objects.equals(product, that.product)
                && Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, attribute);
    }
}
