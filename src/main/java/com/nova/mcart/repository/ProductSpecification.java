package com.nova.mcart.repository;

import com.nova.mcart.entity.Product;
import com.nova.mcart.entity.enums.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    /**
     * Filters for list: only active (non–soft-deleted) products by default.
     * Pass includeInactive true to include soft-deleted products.
     */
    public static Specification<Product> withFilters(Long brandId, Long categoryId, String status, Boolean includeInactive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!Boolean.TRUE.equals(includeInactive)) {
                predicates.add(cb.equal(root.get("isActive"), true));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), ProductStatus.valueOf(status)));
                } catch (IllegalArgumentException ignored) {
                    // invalid status ignored
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
