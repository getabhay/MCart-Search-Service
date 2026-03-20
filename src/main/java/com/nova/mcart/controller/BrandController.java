package com.nova.mcart.controller;

import com.nova.mcart.dto.request.BrandCreateRequest;
import com.nova.mcart.dto.request.BrandUpdateRequest;
import com.nova.mcart.dto.response.BrandResponse;
import com.nova.mcart.service.BrandService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<BrandResponse> create(@RequestBody BrandCreateRequest request) {
        return ResponseEntity.ok(brandService.createBrand(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrandResponse> update(@PathVariable Long id,
                                @RequestBody BrandUpdateRequest request) {
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<BrandResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(brandService.getBrandBySlug(slug));
    }

    @GetMapping
    public ResponseEntity<Page<BrandResponse>> getAll(@RequestParam(required = false) String search,
                                      Pageable pageable) {
        return ResponseEntity.ok(brandService.getAllBrands(search, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Long> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(brandService.uploadAndProcessAsync(file));
    }

    @GetMapping("/bulk-upload/start")
    public ResponseEntity<Long> startBulkJob(@RequestParam String s3Key) {
        return ResponseEntity.ok(brandService.startBulkJob(s3Key));
    }


}
