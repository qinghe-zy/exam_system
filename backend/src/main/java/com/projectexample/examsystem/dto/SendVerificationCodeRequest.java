package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendVerificationCodeRequest {

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Target is required")
    private String targetValue;

    private String username;

    private Long organizationId;
}
