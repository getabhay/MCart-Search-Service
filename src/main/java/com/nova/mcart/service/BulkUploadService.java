package com.nova.mcart.service;

import com.nova.mcart.dto.response.BulkUploadJobResponse;
import com.nova.mcart.dto.response.UploadUrlResponse;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface BulkUploadService {

    UploadUrlResponse generateUploadUrl(String bucket);

    /**
     * Upload CSV file to S3 and return the s3Key for use in entity-specific bulk-upload/start (e.g. brands/bulk-upload/start or categories/bulk-upload/start).
     * Merges the former "generate presigned URL" + "client uploads to S3" into a single step.
     */
    String uploadFile(String bucket, MultipartFile file) throws IOException;

    BulkUploadJobResponse getJobStatus(Long jobId);
}
