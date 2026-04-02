package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrganizationSaveRequest {

    @NotBlank(message = "Organization code is required")
    private String orgCode;

    @NotBlank(message = "Organization name is required")
    private String orgName;

    @NotBlank(message = "Organization type is required")
    private String orgType;

    @NotNull(message = "Parent organization is required")
    private Long parentId;

    @NotNull(message = "Status is required")
    private Integer status;
}
