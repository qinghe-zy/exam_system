package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionBankVO {

    private Long id;
    private String questionCode;
    private String subject;
    private String questionType;
    private String difficultyLevel;
    private String stem;
    private String optionsJson;
    private String answerKey;
    private String analysisText;
    private String knowledgePoint;
    private String chapterName;
    private String sourceName;
    private String tags;
    private Double defaultScore;
    private String reviewerStatus;
    private Integer versionNo;
    private Integer status;
}
