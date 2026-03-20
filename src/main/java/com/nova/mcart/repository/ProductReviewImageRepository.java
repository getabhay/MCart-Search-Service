package com.nova.mcart.repository;

import com.nova.mcart.entity.ProductReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewImageRepository
        extends JpaRepository<ProductReviewImage, Long> {

    List<ProductReviewImage> findByReviewId(Long reviewId);
}
