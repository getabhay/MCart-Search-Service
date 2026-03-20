package com.nova.mcart.controller;

import com.nova.mcart.dto.request.CreateAttributeRequest;
import com.nova.mcart.dto.request.UpdateAttributeRequest;
import com.nova.mcart.entity.Attribute;
import com.nova.mcart.service.AttributeService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @PostMapping
    public ResponseEntity<Attribute> create(
            @RequestBody CreateAttributeRequest request) {
        return ResponseEntity.ok(attributeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attribute> update(
            @PathVariable Long id,
            @RequestBody UpdateAttributeRequest request) {
        return ResponseEntity.ok(attributeService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attribute> getById(@PathVariable Long id) {
        return ResponseEntity.ok(attributeService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<Attribute>> getAll(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(attributeService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attributeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Long> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(attributeService.uploadAndProcessAsync(file));
    }

    @GetMapping("/bulk-upload/start")
    public ResponseEntity<Long> startBulkJob(@RequestParam String s3Key) {
        return ResponseEntity.ok(attributeService.startBulkJob(s3Key));
    }
}
