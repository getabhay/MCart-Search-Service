package com.nova.mcart.repository;

import com.nova.mcart.entity.AttributeValue;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {

    Page<AttributeValue> findByAttributeIdAndIsActiveTrue(Long attributeId, Pageable pageable);
    Optional<AttributeValue> findByAttributeIdAndValue(Long attributeId, String value);
}
