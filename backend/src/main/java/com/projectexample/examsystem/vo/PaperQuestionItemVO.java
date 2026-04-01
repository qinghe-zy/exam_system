package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperQuestionItemVO {

    private Long questionId;
    private Integer sortNo;
    private Double score;
    private Integer requiredFlag;
    private String questionCode;
    private String questionType;
    private String stem;
}
