package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeAnswerItemRequest {

    @NotNull(message = "Answer item id is required")
    private Long answerItemId;

    @NotNull(message = "Score is required")
    private Double scoreAwarded;

    private String reviewComment;
}
