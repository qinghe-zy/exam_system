package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionBankSaveRequest {

    @NotBlank(message = "Question code is required")
    private String questionCode;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Question type is required")
    private String questionType;

    @NotBlank(message = "Difficulty level is required")
    private String difficultyLevel;

    @NotBlank(message = "Question stem is required")
    private String stem;

    private String stemHtml;

    private String materialContent;

    private String attachmentJson;

    private String optionsJson;

    @NotBlank(message = "Answer key is required")
    private String answerKey;

    private String analysisText;
    private String knowledgePoint;
    private String chapterName;
    private String sourceName;
    private String tags;

    @NotNull(message = "Default score is required")
    private Double defaultScore;

    @NotBlank(message = "Reviewer status is required")
    private String reviewerStatus;

    @NotNull(message = "Version number is required")
    private Integer versionNo;

    @NotNull(message = "Status is required")
    private Integer status;
}
