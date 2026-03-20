package com.nova.mcart.controller;

import com.nova.mcart.dto.request.CreateCategoryAttributeRequest;
import com.nova.mcart.entity.CategoryAttribute;
import com.nova.mcart.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/category-attributes")
@RequiredArgsConstructor
public class CategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @PostMapping
    public ResponseEntity<CategoryAttribute> map(
            @RequestBody CreateCategoryAttributeRequest request) {

        return ResponseEntity.ok(
                categoryAttributeService.map(request)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<CategoryAttribute>> getByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(
                    size = 20,
                    sort = "attribute.name",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                categoryAttributeService.getByCategory(categoryId, pageable)
        );
    }

    @GetMapping("/bulk-upload/start")
    public ResponseEntity<Long> startBulkJob(@RequestParam String s3Key) {
        return ResponseEntity.ok(categoryAttributeService.startBulkJob(s3Key));
    }

}
