package com.projectexample.examsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CandidateImportRequest {

    @NotEmpty(message = "At least one candidate is required")
    @Valid
    private List<CandidateImportItemRequest> items;
}
