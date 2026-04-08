package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreAppealVO {

    private Long id;
    private Long scoreRecordId;
    private Long answerSheetId;
    private Long examPlanId;
    private Long userId;
    private String candidateName;
    private String examName;
    private String appealReason;
    private String expectedOutcome;
    private String status;
    private String resolutionAction;
    private String processComment;
    private Long processedBy;
    private String processedByName;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
}
