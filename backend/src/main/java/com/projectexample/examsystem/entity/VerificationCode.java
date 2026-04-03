package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("sys_verification_code")
@EqualsAndHashCode(callSuper = true)
public class VerificationCode extends BaseEntity {

    private String purpose;
    private String channel;

    @TableField("target_value")
    private String targetValue;

    @TableField("verify_code")
    private String verifyCode;

    private String username;

    @TableField("organization_id")
    private Long organizationId;

    @TableField("delivery_trace")
    private String deliveryTrace;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("verified_flag")
    private Integer verifiedFlag;

    @TableField("consumed_flag")
    private Integer consumedFlag;
}
