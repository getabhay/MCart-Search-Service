package com.nova.mcart.dto.response;

import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkUploadJobResponse {

    private Long id;
    private String fileUrl;
    private String resultFileUrl;
    private JobStatus status;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;
    private EntityType entityType;
}
