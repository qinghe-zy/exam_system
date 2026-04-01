package com.projectexample.examsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CandidateAnswerSheetSaveRequest {

    @NotEmpty(message = "At least one answer is required")
    @Valid
    private List<CandidateAnswerItemRequest> answers;
}
