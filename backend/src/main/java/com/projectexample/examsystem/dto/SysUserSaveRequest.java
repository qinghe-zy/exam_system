package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysUserSaveRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Role code is required")
    private String roleCode;

    @NotNull(message = "Organization id is required")
    private Long organizationId;

    private String departmentName;
    private String email;
    private String phone;
    private String candidateNo;
    private String password;

    @NotNull(message = "Status is required")
    private Integer status;
}
