package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GradingReviewRequest {

    @NotBlank(message = "Review action is required")
    private String action;

    private String reviewComment;
}
