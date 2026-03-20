package com.nova.mcart.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiErrorResponse {

    private String message;
    private int status;
    private long timestamp;
}
