package com.nova.mcart.repository;

import com.nova.mcart.entity.ProductRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRatingSummaryRepository
        extends JpaRepository<ProductRatingSummary, Long> {
}
