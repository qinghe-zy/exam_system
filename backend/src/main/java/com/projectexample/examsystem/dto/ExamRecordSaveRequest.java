package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExamRecordSaveRequest {

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Paper name is required")
    private String paperName;

    @NotNull(message = "Attempt date is required")
    private LocalDate attemptDate;

    @NotNull(message = "Score is required")
    private Double scoreValue;

    @NotNull(message = "Status is required")
    private Integer status;
}
