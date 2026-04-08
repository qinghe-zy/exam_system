package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateScoreItemVO {

    private Long questionId;
    private Integer questionOrder;
    private String questionCode;
    private String questionType;
    private String stem;
    private String stemHtml;
    private String materialContent;
    private String attachmentJson;
    private String optionsJson;
    private String knowledgePoint;
    private String chapterName;
    private String answerContent;
    private String referenceAnswer;
    private String analysisText;
    private Double maxScore;
    private Double scoreAwarded;
    private String status;
    private Integer reviewLaterFlag;
    private String reviewComment;
}
