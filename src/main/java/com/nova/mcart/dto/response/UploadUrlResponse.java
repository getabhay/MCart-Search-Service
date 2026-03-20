package com.nova.mcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadUrlResponse {

    private String uploadUrl;
    private String s3Key;
}
