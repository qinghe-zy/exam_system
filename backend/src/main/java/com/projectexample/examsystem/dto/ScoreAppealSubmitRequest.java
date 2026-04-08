package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScoreAppealSubmitRequest {

    @NotBlank(message = "Appeal reason is required")
    private String appealReason;

    private String expectedOutcome;
}
