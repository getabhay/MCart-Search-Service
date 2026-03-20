package com.nova.mcart.repository;

import com.nova.mcart.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySlug(String slug);

    Optional<Product> findBySlug(String slug);

    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category", "ratingSummary"})
    @Query("select p from Product p")
    List<Product> findAllBaseForIndexing();

    @EntityGraph(attributePaths = {"brand", "category", "ratingSummary"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findBaseForIndexingById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "brand",
            "category",
            "ratingSummary"
    })
    @Query("select p from Product p where p.isActive = true")
    Page<Product> findPageForIndexing(Pageable pageable);

    @Query("""
            select p from Product p
            left join fetch p.brand
            left join fetch p.category
            left join fetch p.variants
            where p.id = :id
            """)
    Optional<Product> findByIdWithBrandAndCategoryAndVariants(@Param("id") Long id);

    @Query("""
            select p from Product p
            left join fetch p.brand
            left join fetch p.category
            left join fetch p.variants
            where p.slug = :slug
            """)
    Optional<Product> findBySlugWithBrandAndCategoryAndVariants(@Param("slug") String slug);

    @Query("""
        select distinct p from Product p
        left join fetch p.productAttributes pa
        left join fetch pa.attribute a
        left join fetch pa.attributeValue av
        where p.id = :id
        """)
    Optional<Product> findByIdWithProductAttributesAndMeta(@Param("id") Long id);

}
