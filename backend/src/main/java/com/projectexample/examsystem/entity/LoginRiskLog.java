package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_login_risk_log")
@EqualsAndHashCode(callSuper = true)
public class LoginRiskLog extends BaseEntity {

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
