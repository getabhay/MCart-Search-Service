package com.nova.mcart.repository;

import com.nova.mcart.entity.VariantAttribute;
import com.nova.mcart.entity.compositeKey.VariantAttributeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantAttributeRepository
        extends JpaRepository<VariantAttribute, VariantAttributeId> {

    List<VariantAttribute> findByVariantId(Long variantId);
}
