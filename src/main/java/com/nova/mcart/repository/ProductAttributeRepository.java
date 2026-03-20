package com.nova.mcart.repository;

import com.nova.mcart.entity.ProductAttribute;
import com.nova.mcart.entity.compositeKey.ProductAttributeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductAttributeRepository
        extends JpaRepository<ProductAttribute, ProductAttributeId> {

    List<ProductAttribute> findByProductId(Long productId);

    @Query("""
        select pa from ProductAttribute pa
        join fetch pa.attribute a
        join fetch pa.attributeValue av
        where pa.product.id in :productIds
        """)
    List<ProductAttribute> findAllByProductIdInFetch(@Param("productIds") List<Long> productIds);

    @Query("""
        select pa from ProductAttribute pa
        left join fetch pa.attribute a
        left join fetch pa.attributeValue av
        where pa.product.id in :productIds
    """)
    List<ProductAttribute> findAllForIndexingByProductIds(List<Long> productIds);
}
