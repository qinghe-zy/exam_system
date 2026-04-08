package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScoreAppealProcessRequest {

    @NotBlank(message = "Process action is required")
    private String action;

    private String processComment;
}
