package com.nova.mcart.controller;

import com.nova.mcart.dto.request.CreateAttributeValueRequest;
import com.nova.mcart.entity.AttributeValue;
import com.nova.mcart.service.AttributeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attribute-values")
@RequiredArgsConstructor
public class AttributeValueController {

    private final AttributeValueService attributeValueService;

    @PostMapping
    public ResponseEntity<AttributeValue> create(
            @RequestBody CreateAttributeValueRequest request) {

        return ResponseEntity.ok(attributeValueService.create(request));
    }

    @GetMapping("/{attributeId}")
    public ResponseEntity<Page<AttributeValue>> getByAttribute(
            @PathVariable Long attributeId,
            @PageableDefault(
                    size = 20,
                    sort = "sortOrder",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                attributeValueService.getByAttribute(attributeId, pageable)
        );
    }

    @GetMapping("/bulk-upload/start")
    public ResponseEntity<Long> startBulkJob(@RequestParam String s3Key) {
        return ResponseEntity.ok(attributeValueService.startBulkJob(s3Key));
    }
}
