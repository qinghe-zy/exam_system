package com.projectexample.examsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgePointAutoGroupRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String difficultyLevel;

    private String questionType;

    @Valid
    @NotEmpty(message = "Knowledge point quotas are required")
    private List<KnowledgePointQuotaItemRequest> quotas;
}
