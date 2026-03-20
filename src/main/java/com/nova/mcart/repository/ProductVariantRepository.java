package com.nova.mcart.repository;

import com.nova.mcart.entity.ProductVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);
    List<ProductVariant> findByProductId(Long productId);

    @Query("""
        select distinct v from ProductVariant v
        left join fetch v.variantAttributes va
        left join fetch va.attribute
        left join fetch va.attributeValue
        where v.product.id in :productIds
        """)
    List<ProductVariant> findAllByProductIdInFetch(@Param("productIds") List<Long> productIds);

    @Query("""
        select v from ProductVariant v
        left join fetch v.variantAttributes va
        left join fetch va.attribute a
        left join fetch va.attributeValue av
        where v.product.id in :productIds
    """)
    List<ProductVariant> findAllForIndexingByProductIds(List<Long> productIds);
}

