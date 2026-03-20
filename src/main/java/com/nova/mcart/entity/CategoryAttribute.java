package com.nova.mcart.entity;

import com.nova.mcart.entity.compositeKey.CategoryAttributeId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "category_attribute",
        indexes = {
                @Index(name = "idx_cat_attr_category", columnList = "category_id"),
                @Index(name = "idx_cat_attr_attribute", columnList = "attribute_id")
        }
)
public class CategoryAttribute {

    @EmbeddedId
    private CategoryAttributeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private Attribute attribute;

    @Column(name = "is_variant_level")
    private Boolean isVariantLevel;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "is_filterable")
    private Boolean isFilterable;
}
