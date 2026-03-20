package com.nova.mcart.service.impl;

import com.nova.mcart.common.ProductMapper;
import com.nova.mcart.common.bulk.ProductBulkProcessor;
import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.dto.request.ProductUpdateRequest;
import com.nova.mcart.dto.response.ProductResponse;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.Product;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.entity.enums.ProductStatus;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.repository.ProductRepository;
import com.nova.mcart.repository.VariantAttributeRepository;
import com.nova.mcart.service.ProductCommandService;
import com.nova.mcart.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final VariantAttributeRepository variantAttributeRepository;

    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final ObjectProvider<ProductBulkProcessor> productBulkProcessorProvider;

    private final ProductMapper productMapper;
    private final ProductCommandService productCommandService;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        return productCommandService.createProduct(request);
    }

    @Override
    @Transactional
    public Long startBulkUpload(String s3Key) {

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.PRODUCT);

        job = bulkUploadJobRepository.save(job);
        Long jobId = job.getId();

        // ✅ Start async only after the PENDING row is committed
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    productBulkProcessorProvider.getObject().process(jobId);
                }
            });
        } else {
            productBulkProcessorProvider.getObject().process(jobId);
        }

        return jobId;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        return getProductById(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
//
//        Product product = productRepository.findBySlug(slug)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Product product = productRepository.findBySlugWithBrandAndCategoryAndVariants(slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Product productWithAttrs = productRepository.findByIdWithProductAttributesAndMeta(product.getId())
                .orElseThrow(() -> new RuntimeException("Product not found (attrs): " + slug));
        product.setProductAttributes(productWithAttrs.getProductAttributes());

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(Pageable pageable) {
        return productRepository.findAll(pageable).map(p -> getProductById(p.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(String search, Pageable pageable) {
        // ⚠️ Your previous code tried to filter a Page like a Stream (won't work correctly).
        // For now: keep it simple (or create repository method containsIgnoreCase)
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        // keep your existing implementation
        throw new UnsupportedOperationException("Keep your existing updateProduct code");
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setStatus(ProductStatus.INACTIVE);
    }

    @Override
    @Transactional
    public void changeStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Product not found"));

        Product product = productRepository.findByIdWithBrandAndCategoryAndVariants(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Product productWithAttrs = productRepository.findByIdWithProductAttributesAndMeta(id)
                .orElseThrow(() -> new RuntimeException("Product not found (attrs): " + id));
        product.setProductAttributes(productWithAttrs.getProductAttributes());
        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return productMapper.map(product,
                variantAttributeRepository.findAll()
        );
    }
}
