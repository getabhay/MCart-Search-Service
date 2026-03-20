package com.nova.mcart.entity;

import com.nova.mcart.entity.compositeKey.ProductAttributeId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "product_attribute",
        indexes = {
                @Index(name = "idx_prod_attr_product", columnList = "product_id"),
                @Index(name = "idx_prod_attr_attribute", columnList = "attribute_id"),
                @Index(name = "idx_prod_attr_value", columnList = "attribute_value_id")
        }
)
@IdClass(ProductAttributeId.class)
@Getter
@Setter
public class ProductAttribute {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_value_id", nullable = false)
    private AttributeValue attributeValue;
}
