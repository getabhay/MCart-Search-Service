package com.nova.mcart.controller;


import com.nova.mcart.dto.response.BulkUploadJobResponse;
import com.nova.mcart.dto.response.UploadUrlResponse;
import com.nova.mcart.service.BulkUploadService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;

    @GetMapping("/bulk-upload/status/{jobId}")
    public ResponseEntity<BulkUploadJobResponse> getStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(bulkUploadService.getJobStatus(jobId));
    }

    /**
     * Legacy: generate presigned URL for client-side upload.
     * Prefer {@link #uploadFile(String, MultipartFile)} to merge upload into one step.
     */
    @GetMapping("/generate-upload-url")
    public ResponseEntity<UploadUrlResponse> generateUploadUrl(@RequestParam String bucket) {
        return ResponseEntity.ok(bulkUploadService.generateUploadUrl(bucket));
    }

    /**
     * Upload CSV to S3 in one step. Returns s3Key to use in entity-specific start endpoint.
     * Example: POST /api/v1/bulk-upload/upload?bucket=brand with multipart file → then GET /api/v1/brands/bulk-upload/start?s3Key=...
     */
    @PostMapping(value = "/bulk-upload/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(
            @RequestParam String bucket,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(bulkUploadService.uploadFile(bucket, file));
    }
}
