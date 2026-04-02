package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgePointAnalysisVO {

    private String knowledgePoint;
    private Double averageScoreRate;
    private Long answerCount;
}
