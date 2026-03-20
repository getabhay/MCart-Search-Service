package com.nova.mcart.service;

/**
 * Starts product bulk upload jobs. Kept separate from ProductService to avoid
 * circular dependency with ProductBulkProcessor (processor needs ProductService).
 */
public interface ProductBulkService {

    /**
     * Start async bulk product create from CSV at the given S3 key.
     * CSV format: name,brandId,categoryId,status,sku,mrp,sellingPrice[,shortDescription]
     *
     * @param s3Key S3 key of the CSV file
     * @return bulk job id; poll GET /api/v1/bulk-upload/status/{jobId} for status
     */
    Long startBulkJob(String s3Key);
}
