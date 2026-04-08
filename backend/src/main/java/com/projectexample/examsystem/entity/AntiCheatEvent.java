package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_anti_cheat_event")
@EqualsAndHashCode(callSuper = true)
public class AntiCheatEvent extends BaseEntity {

    private Long examPlanId;
    private Long answerSheetId;
    private Long userId;
    private String eventType;
    private String severity;
    private Integer leaveCount;
    private Integer triggeredAutoSave;
    private Integer saveVersion;
    private String clientIp;
    private String deviceFingerprint;
    private String deviceInfo;
    private String detailText;
    private LocalDateTime occurredAt;
}
