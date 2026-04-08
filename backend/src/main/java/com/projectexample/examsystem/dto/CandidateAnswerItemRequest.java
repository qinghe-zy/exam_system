package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CandidateAnswerItemRequest {

    @NotNull(message = "Question id is required")
    private Long questionId;

    private String answerContent;

    private Integer reviewLaterFlag;
}
