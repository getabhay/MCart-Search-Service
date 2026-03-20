package com.nova.mcart.entity;

import com.nova.mcart.entity.compositeKey.VariantAttributeId;
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
        name = "variant_attribute",
        indexes = {
                @Index(name = "idx_var_attr_variant", columnList = "variant_id"),
                @Index(name = "idx_var_attr_attribute", columnList = "attribute_id"),
                @Index(name = "idx_var_attr_value", columnList = "attribute_value_id")
        }
)
@IdClass(VariantAttributeId.class)
@Getter
@Setter
public class VariantAttribute {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_value_id", nullable = false)
    private AttributeValue attributeValue;

}
