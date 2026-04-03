package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiQuestionPolishVO {

    private String improvedStem;
    private String improvedAnswerKey;
    private String improvedAnalysisText;
    private String suggestedOptionsJson;
    private String aiHint;
}
