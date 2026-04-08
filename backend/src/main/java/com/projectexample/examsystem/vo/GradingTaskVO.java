package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GradingTaskVO {

    private Long answerSheetId;
    private String examName;
    private String candidateName;
    private LocalDateTime submittedAt;
    private Double objectiveScore;
    private Integer subjectiveQuestionCount;
    private Integer pendingQuestionCount;
    private String status;
    private String reviewStatus;
    private String appealStatus;
}
