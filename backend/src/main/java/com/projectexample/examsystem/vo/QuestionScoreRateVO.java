package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionScoreRateVO {

    private Long questionId;
    private String questionCode;
    private String stem;
    private Double averageScoreRate;
    private Long answerCount;
}
