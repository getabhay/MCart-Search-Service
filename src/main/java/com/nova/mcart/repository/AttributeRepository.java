package com.nova.mcart.repository;

import com.nova.mcart.entity.Attribute;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    boolean existsBySlug(String slug);

    Optional<Attribute> findBySlugAndIsActiveTrue(String slug);

    Page<Attribute> findByIsActiveTrue(Pageable pageable);
}
