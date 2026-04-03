package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateAnswerItemVO {

    private Long answerItemId;
    private Long questionId;
    private Integer questionOrder;
    private String questionCode;
    private String questionType;
    private String stem;
    private String stemHtml;
    private String materialContent;
    private String attachmentJson;
    private String optionsJson;
    private Double maxScore;
    private String answerContent;
    private Double scoreAwarded;
    private String status;
    private Integer markedFlag;
    private String reviewComment;
}
