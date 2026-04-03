package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VerificationCodeSendVO {

    private String purpose;
    private String channel;
    private String targetValue;
    private LocalDateTime expiresAt;
    private String deliveryTrace;
    private String mockCode;
}
