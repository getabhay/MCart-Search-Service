package com.nova.mcart.entity;

import com.nova.mcart.common.entity.BaseAuditEntity;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bulk_upload_job")
public class BulkUploadJob extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileUrl;

    private String resultFileUrl;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Integer totalRecords;

    private Integer successCount;

    private Integer failureCount;

    @Enumerated(EnumType.STRING)
    private EntityType entityType; // BRAND, PRODUCT, CATEGORY
}
