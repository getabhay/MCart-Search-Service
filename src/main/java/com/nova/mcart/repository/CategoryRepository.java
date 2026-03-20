package com.nova.mcart.repository;

import com.nova.mcart.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentId(Long parentId);

    boolean existsBySlug(String slug);

    List<Category> findAllByIsActiveTrueOrderByPathAsc();

    List<Category> findByPathStartingWithAndIsActiveTrue(String path);

    Page<Category> findByIsActiveTrueAndNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<Category> findByIsActiveTrue(Pageable pageable);

    Optional<Category> findByNameIgnoreCase(String name);
}
