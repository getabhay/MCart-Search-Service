package com.nova.mcart.controller;

import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.dto.response.ProductResponse;
import com.nova.mcart.search.ProductIndexService;
import com.nova.mcart.service.ProductService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final ProductIndexService productIndexService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @RequestBody ProductCreateRequest request) {

        return ResponseEntity.ok(
                productService.createProduct(request)
        );
    }

    @GetMapping("/bulk-upload/start")
    public ResponseEntity<Long> startBulkUpload(
            @RequestParam String s3Key) {

        Long jobId = productService.startBulkUpload(s3Key);

        return ResponseEntity.ok(jobId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getBySlug(
            @PathVariable String slug) {

        return ResponseEntity.ok(
                productService.getBySlug(slug)
        );
    }

    @GetMapping("/search/all")
    public ResponseEntity<Page<ProductResponse>> getAll(@RequestParam(required = false) String search,
                                                      Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(search, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<ProductResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(productService.list(pageable));
    }

    @GetMapping("/es/reindex")
    public String reindex() throws IOException {
        productIndexService.bulkReindexAll();
        return "Reindex done";
    }

}
