package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationComparisonVO {

    private String organizationName;
    private Long candidateCount;
    private Double averageScore;
    private Double passRate;
    private Double excellentRate;
}
