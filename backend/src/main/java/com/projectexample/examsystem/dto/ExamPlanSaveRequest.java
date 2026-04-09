package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamPlanSaveRequest {

    @NotBlank(message = "Exam code is required")
    private String examCode;

    @NotBlank(message = "Exam name is required")
    private String examName;

    private String examMode;
    private String batchLabel;
    private String examRoom;
    private Long sourceExamPlanId;

    @NotNull(message = "Paper id is required")
    private Long paperId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @NotNull(message = "Pass score is required")
    private Double passScore;

    @NotBlank(message = "Candidate scope is required")
    private String candidateScope;

    @NotNull(message = "Attempt limit is required")
    private Integer attemptLimit;

    private String examPassword;
    private Integer lateEntryMinutes;
    private Integer signInRequired;
    private Integer signInStartMinutes;
    private Integer earlySubmitMinutes;
    private Integer autoSubmitEnabled;
    private String antiCheatLevel;
    private String instructionText;

    @NotNull(message = "Status is required")
    private Integer status;

    @NotNull(message = "Publish status is required")
    private Integer publishStatus;

    @NotEmpty(message = "At least one candidate is required")
    private List<Long> candidateUserIds;
}
