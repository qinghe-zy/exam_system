package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamQualityInsightVO {

    private Long examPlanId;
    private String examName;
    private Integer candidateCount;
    private Integer submittedCount;
    private Integer gradedCount;
    private Double averageScore;
    private Double passRate;
    private String level;
    private String summary;
    private String risk;
}
