package com.projectexample.examsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamPaperSaveRequest {

    @NotBlank(message = "Paper code is required")
    private String paperCode;

    @NotBlank(message = "Paper name is required")
    private String paperName;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Assembly mode is required")
    private String assemblyMode;

    private String descriptionText;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @NotNull(message = "Total score is required")
    private Double totalScore;

    @NotNull(message = "Pass score is required")
    private Double passScore;

    @NotNull(message = "Publish status is required")
    private Integer publishStatus;

    @NotEmpty(message = "At least one paper question is required")
    @Valid
    private List<PaperQuestionItemRequest> questionItems;
}
