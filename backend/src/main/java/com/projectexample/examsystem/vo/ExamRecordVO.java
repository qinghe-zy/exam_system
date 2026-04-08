package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamRecordVO {

    private Long id;
    private Long examPlanId;
    private Long answerSheetId;
    private String candidateName;
    private String examName;
    private String paperName;
    private LocalDateTime submittedAt;
    private Double objectiveScore;
    private Double subjectiveScore;
    private Double finalScore;
    private Integer passedFlag;
    private Integer publishedFlag;
    private String reviewStatus;
    private String appealStatus;
    private String status;
}
