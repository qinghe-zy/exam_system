package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Organization is required")
    private Long organizationId;

    private String departmentName;

    @Email(message = "Email is invalid")
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Verification code is required")
    private String verificationCode;

    @NotBlank(message = "Verification channel is required")
    private String verificationChannel;
}
