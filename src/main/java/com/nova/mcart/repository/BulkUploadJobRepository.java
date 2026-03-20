package com.nova.mcart.repository;

import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BulkUploadJobRepository extends JpaRepository<BulkUploadJob, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update BulkUploadJob j
           set j.status = com.nova.mcart.entity.enums.JobStatus.PROCESSING
         where j.id = :id
           and j.status = com.nova.mcart.entity.enums.JobStatus.PENDING
    """)
    int markProcessingIfPending(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update BulkUploadJob j
           set j.status = :status,
               j.totalRecords = :total,
               j.successCount = :success,
               j.failureCount = :failure,
               j.resultFileUrl = :resultFileUrl
         where j.id = :id
    """)
    int updateFinal(
            @Param("id") Long id,
            @Param("status") JobStatus status,
            @Param("total") Integer total,
            @Param("success") Integer success,
            @Param("failure") Integer failure,
            @Param("resultFileUrl") String resultFileUrl
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update BulkUploadJob j
           set j.status = :status
         where j.id = :id
    """)
    int updateStatus(@Param("id") Long id, @Param("status") JobStatus status);

    // Optional: nice for safety/observability
    @Query("""
        select j.fileUrl
          from BulkUploadJob j
         where j.id = :id
    """)
    String findFileUrlById(@Param("id") Long id);
}
