package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginRiskLogVO {

    private Long id;
    private String username;
    private Long userId;
    private String roleCode;
    private Integer successFlag;
    private String clientIp;
    private String userAgent;
    private String deviceFingerprint;
    private String deviceInfo;
    private String riskLevel;
    private String riskReason;
    private LocalDateTime loginAt;
}
