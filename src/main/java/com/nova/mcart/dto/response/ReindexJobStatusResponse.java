package com.nova.mcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReindexJobStatusResponse {

    private String jobId;

    private String status; // RUNNING | COMPLETED | FAILED
    private long totalRead;
    private long totalIndexed;

    private String error;
}
