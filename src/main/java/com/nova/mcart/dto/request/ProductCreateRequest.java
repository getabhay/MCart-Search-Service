package com.nova.mcart.dto.request;

import com.nova.mcart.entity.enums.ImageType;
import com.nova.mcart.entity.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest{

    /* ==============================
       ========== BASIC INFO =========
       ============================== */

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @NotNull
    private Long brandId;

    @NotNull
    private Long categoryId;

    @NotNull
    private ProductStatus status;

    /* ==============================
       ===== PRODUCT ATTRIBUTES =====
       ============================== */

    @Valid
    private List<ProductAttributeRequest> attributes;

    /* ==============================
       ========= VARIANTS ===========
       ============================== */

    @NotNull
    @Size(min = 1)
    @Valid
    private List<VariantRequest> variants;

    /* ==============================
       ========= IMAGES (OPTIONAL) ==
       ============================== */

    @Valid
    private List<ProductImageRequest> images;

    /* ======================================================
       ================= INNER DTO CLASSES ==================
       ====================================================== */

    @Getter
    @Setter
    public static class ProductAttributeRequest {

        @NotBlank
        private String attributeSlug;

        @NotBlank
        private String value;
    }

    @Getter
    @Setter
    public static class VariantRequest {

        @NotBlank
        private String sku;

        @NotNull
        @Positive
        private BigDecimal mrp;

        @NotNull
        @Positive
        private BigDecimal sellingPrice;

        @NotNull
        private ProductStatus status;

        @NotNull
        @Positive
        private Integer stockQuantity;

        @Valid
        private List<VariantAttributeRequest> attributes;

        @Valid
        private List<ProductImageRequest> images;
    }

    @Getter
    @Setter
    public static class VariantAttributeRequest {

        @NotNull
        private Long attributeId;

        @NotBlank
        private String value;
    }

    @Getter
    @Setter
    public static class ProductImageRequest {

        @NotBlank
        private String fileKey;
        // Only store S3 key like:
        // products/123/variants/456/image1.jpg

        private ImageType imageType;
        // THUMBNAIL / GALLERY / SWATCH

        private Integer displayOrder;
    }
}
