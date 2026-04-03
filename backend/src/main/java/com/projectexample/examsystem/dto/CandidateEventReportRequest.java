package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CandidateEventReportRequest {

    private Long answerSheetId;

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Severity is required")
    private String severity;

    private Integer leaveCount;
    private Integer triggeredAutoSave;
    private Integer saveVersion;
    private String detailText;
}
