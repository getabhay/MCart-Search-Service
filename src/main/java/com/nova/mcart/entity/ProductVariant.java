package com.nova.mcart.entity;

import com.nova.mcart.common.entity.BaseAuditEntity;
import com.nova.mcart.entity.enums.ProductStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "product_variant",
        indexes = {
                @Index(name = "idx_variant_product", columnList = "product_id"),
                @Index(name = "idx_variant_sku", columnList = "sku"),
                @Index(name = "idx_variant_active", columnList = "is_active")
        }
)
public class ProductVariant extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // SKU (manual or auto generated)
    @Column(nullable = false, unique = true)
    private String sku;

    // MRP (Maximum Retail Price)
    @Column(name = "mrp", precision = 12, scale = 2)
    private BigDecimal mrp;

    // Actual selling price
    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    // Stock per variant
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "is_active")
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status;

    // Attribute combinations (Size=M, Color=Red)
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VariantAttribute> variantAttributes;

    // Optional: Variant specific images
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;
}
