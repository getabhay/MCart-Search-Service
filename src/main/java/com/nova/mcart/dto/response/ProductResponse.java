package com.nova.mcart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductResponse {

    /* ==============================
       ========== BASIC INFO =========
       ============================== */

    private Long id;

    private String name;

    private String slug;

    private String shortDescription;

    private String description;

    private String status;

    /* ==============================
       ========= BRAND INFO =========
       ============================== */

    private BrandInfo brand;

    /* ==============================
       ======== CATEGORY INFO =======
       ============================== */

    private CategoryInfo category;

    /* ==============================
       ===== PRICE AGGREGATION ======
       ============================== */

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    /* ==============================
       ===== PRODUCT ATTRIBUTES =====
       ============================== */

    private List<ProductAttributeResponse> attributes;

    /* ==============================
       ========= VARIANTS ===========
       ============================== */

    private List<VariantResponse> variants;

    /* ==============================
       ========= IMAGES =============
       ============================== */

    private List<ProductImageResponse> images;

    /* ==============================
       ====== RATING SUMMARY ========
       ============================== */

    private RatingSummary ratingSummary;

    /* ======================================================
       ================= INNER CLASSES ======================
       ====================================================== */

    @Getter
    @Setter
    public static class BrandInfo {

        private Long id;
        private String name;
        private String slug;
    }

    @Getter
    @Setter
    public static class CategoryInfo {

        private Long id;
        private String name;
        private String slug;
        private String path;
    }

    @Getter
    @Setter
    public static class ProductAttributeResponse {

        private Long attributeId;
        private String attributeName;
        private String value;
    }

    @Getter
    @Setter
    public static class VariantResponse {

        private Long id;
        private String sku;
        private Integer stockQuantity;
        private BigDecimal mrp;
        private BigDecimal sellingPrice;
        private String status;

        private List<VariantAttributeResponse> attributes;
        private List<ProductImageResponse> images;
    }

    @Getter
    @Setter
    public static class VariantAttributeResponse {

        private Long attributeId;
        private String attributeName;
        private String value;
    }

    @Getter
    @Setter
    public static class ProductImageResponse {

        private String imageUrl;
        // Full CDN URL (generated in service layer)

        private String imageType;
        private Integer displayOrder;
    }

    @Getter
    @Setter
    public static class RatingSummary {

        private BigDecimal averageRating;
        private Integer totalRatings;
    }
}
