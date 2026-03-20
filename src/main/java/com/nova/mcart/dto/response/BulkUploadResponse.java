package com.nova.mcart.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for bulk upload of products.
 * Contains row-wise summary and failed rows for re-upload.
 */
@Getter
@Setter
@NoArgsConstructor
public class BulkUploadResponse {

    /**
     * Summary for each CSV row (success/failure)
     */
    private List<BulkRowStatus> summary = new ArrayList<>();

    /**
     * Original CSV rows that failed validation/insert
     */
    private List<String> failedRows = new ArrayList<>();

    /**
     * Row-wise status of a single CSV row
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class BulkRowStatus {
        /**
         * Row number in CSV (including header)
         */
        private int rowNumber;

        /**
         * "SUCCESS" or "FAILURE"
         */
        private String status;

        /**
         * Reason for failure, null if success
         */
        private String reason;

        public BulkRowStatus(int rowNumber, String status, String reason) {
            this.rowNumber = rowNumber;
            this.status = status;
            this.reason = reason;
        }
    }
}
