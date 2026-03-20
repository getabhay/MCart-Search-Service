package com.nova.mcart.service;

import com.nova.mcart.dto.request.BrandCreateRequest;
import com.nova.mcart.dto.request.BrandUpdateRequest;
import com.nova.mcart.dto.response.BrandResponse;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface BrandService {

    BrandResponse createBrand(BrandCreateRequest request);

    BrandResponse updateBrand(Long id, BrandUpdateRequest request);

    BrandResponse getBrandById(Long id);

    BrandResponse getBrandBySlug(String slug);

    Page<BrandResponse> getAllBrands(String search, Pageable pageable);

    void deleteBrand(Long id);

    Long uploadAndProcessAsync(MultipartFile file) throws IOException;

    Long startBulkJob(String s3Key);

}
