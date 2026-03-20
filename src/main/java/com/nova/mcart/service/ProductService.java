package com.nova.mcart.service;

import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.dto.request.ProductUpdateRequest;
import com.nova.mcart.dto.response.ProductResponse;
import com.nova.mcart.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /* ==============================
       ========== COMMANDS ==========
       ============================== */

    ProductResponse createProduct(ProductCreateRequest request);

    Long startBulkUpload(String s3Key);

    void updateProduct(Long productId, ProductUpdateRequest request);

    ProductResponse getById(Long productId);
    ProductResponse getProductById(Long id);
    ProductResponse getBySlug(String slug);

    Page<ProductResponse> list(Pageable pageable);

    Page<ProductResponse> getAllProducts(String search, Pageable pageable);

    void deleteProduct(Long productId);

    void changeStatus(Long productId, ProductStatus status);

}