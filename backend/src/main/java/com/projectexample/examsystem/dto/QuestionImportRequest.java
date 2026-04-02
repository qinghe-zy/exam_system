package com.projectexample.examsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuestionImportRequest {

    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<QuestionBankSaveRequest> questions;
}
