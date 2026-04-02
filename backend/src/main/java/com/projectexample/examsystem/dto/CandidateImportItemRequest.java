package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CandidateImportItemRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Candidate number is required")
    private String candidateNo;

    @NotNull(message = "Organization id is required")
    private Long organizationId;

    private String departmentName;
    private String email;
    private String phone;
}
