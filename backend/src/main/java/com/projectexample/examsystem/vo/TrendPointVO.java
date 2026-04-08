package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendPointVO {

    private Long examPlanId;
    private String examName;
    private String periodLabel;
    private Double averageScore;
    private Double passRate;
    private Double excellentRate;
}
