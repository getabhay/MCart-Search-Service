package com.nova.mcart.dto.request;

import com.nova.mcart.entity.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateRequest {

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

    @Valid
    private List<ProductCreateRequest.ProductAttributeRequest> attributes;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<VariantRequest> variants;

    @Valid
    private List<ProductCreateRequest.ProductImageRequest> images;

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
        @Positive
        private Integer stockQuantity;

        @NotNull
        private ProductStatus status;

        @Valid
        private List<ProductCreateRequest.VariantAttributeRequest> attributes;

        @Valid
        private List<ProductCreateRequest.ProductImageRequest> images;
    }
}
