package com.nova.mcart.entity;

import com.nova.mcart.common.entity.BaseAuditEntity;
import com.nova.mcart.entity.enums.AttributeType;
import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "attribute",
        indexes = {
                @Index(name = "idx_attribute_slug", columnList = "slug"),
                @Index(name = "idx_attribute_filterable", columnList = "is_filterable"),
                @Index(name = "idx_attribute_variant", columnList = "is_variant"),
                @Index(name = "idx_attribute_active", columnList = "is_active")
        }
)
public class Attribute extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private AttributeType dataType;

    @Column(name = "is_filterable")
    private Boolean isFilterable;

    @Column(name = "is_searchable")
    private Boolean isSearchable;

    @Column(name = "is_variant")
    private Boolean isVariant;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    private List<CategoryAttribute> categoryAttributes;

    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    private List<ProductAttribute> productAttributes;

    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    private List<VariantAttribute> variantAttributes;
}
