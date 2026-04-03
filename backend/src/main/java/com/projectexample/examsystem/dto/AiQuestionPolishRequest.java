package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiQuestionPolishRequest {

    @NotBlank(message = "学科不能为空")
    private String subject;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotBlank(message = "难度不能为空")
    private String difficultyLevel;

    @NotBlank(message = "题干不能为空")
    private String stem;

    private String optionsJson;
    private String answerKey;
    private String analysisText;
    private String knowledgePoint;
    private String chapterName;
}
