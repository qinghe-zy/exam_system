package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AntiCheatEventVO {

    private Long id;
    private Long examPlanId;
    private String examName;
    private Long answerSheetId;
    private Long userId;
    private String candidateName;
    private String eventType;
    private String severity;
    private Integer leaveCount;
    private Integer triggeredAutoSave;
    private Integer saveVersion;
    private String detailText;
    private LocalDateTime occurredAt;
}
