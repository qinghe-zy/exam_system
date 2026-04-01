package com.projectexample.examsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GradingSubmitRequest {

    @NotEmpty(message = "At least one grading item is required")
    @Valid
    private List<GradeAnswerItemRequest> gradeItems;
}
