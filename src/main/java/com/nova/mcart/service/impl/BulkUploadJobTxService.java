package com.nova.mcart.service.impl;

import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.BulkUploadJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BulkUploadJobTxService {

    private final BulkUploadJobRepository bulkUploadJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessingIfPending(Long jobId) {
        return bulkUploadJobRepository.markProcessingIfPending(jobId) > 0;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public BulkUploadJob getJobOrNull(Long jobId) {
        return bulkUploadJobRepository.findById(jobId).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFinal(Long jobId,
                            JobStatus status,
                            Integer total,
                            Integer success,
                            Integer failure,
                            String resultFileUrl) {

        bulkUploadJobRepository.updateFinal(jobId, status, total, success, failure, resultFileUrl);
    }
}
