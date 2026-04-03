package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Verification code is required")
    private String verificationCode;

    @NotBlank(message = "Verification channel is required")
    private String verificationChannel;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
