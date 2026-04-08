package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamPerformanceVO {

    private Long examPlanId;
    private String examName;
    private Integer candidateCount;
    private Integer submittedCount;
    private Integer gradedCount;
    private Double averageScore;
    private Double highestScore;
    private Double lowestScore;
    private Double passRate;
    private Double excellentRate;
}
