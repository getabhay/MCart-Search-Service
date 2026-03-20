package com.nova.mcart.repository;

import com.nova.mcart.entity.CategoryAttribute;
import com.nova.mcart.entity.compositeKey.CategoryAttributeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryAttributeRepository
        extends JpaRepository<CategoryAttribute, CategoryAttributeId> {

    Page<CategoryAttribute> findByIdCategoryId(
            Long categoryId,
            Pageable pageable
    );

    boolean existsById(CategoryAttributeId id);
}
