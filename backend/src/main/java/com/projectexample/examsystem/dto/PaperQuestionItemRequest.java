package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperQuestionItemRequest {

    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotNull(message = "Sort order is required")
    private Integer sortNo;

    @NotNull(message = "Score is required")
    private Double score;

    @NotNull(message = "Required flag is required")
    private Integer requiredFlag;
}
