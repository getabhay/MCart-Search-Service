package com.nova.mcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReindexStartResponse {
    private String jobId;
    private String message;
}
