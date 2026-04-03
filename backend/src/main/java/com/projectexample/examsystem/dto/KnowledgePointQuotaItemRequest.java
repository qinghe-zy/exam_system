package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgePointQuotaItemRequest {

    @NotBlank(message = "Knowledge point is required")
    private String knowledgePoint;

    @NotNull(message = "Question count is required")
    private Integer questionCount;
}
