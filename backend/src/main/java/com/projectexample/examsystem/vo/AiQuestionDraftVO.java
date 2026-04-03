package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiQuestionDraftVO {

    private String subject;
    private String questionType;
    private String difficultyLevel;
    private String stem;
    private String optionsJson;
    private String answerKey;
    private String analysisText;
    private String knowledgePoint;
    private String chapterName;
    private String tags;
    private Double defaultScore;
    private String aiHint;
}
