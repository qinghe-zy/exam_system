package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.LoginRequest;
import com.projectexample.examsystem.dto.PasswordResetRequest;
import com.projectexample.examsystem.dto.RegisterRequest;
import com.projectexample.examsystem.dto.SendVerificationCodeRequest;
import com.projectexample.examsystem.vo.AuthTokenVO;
import com.projectexample.examsystem.vo.AuthRegisterOptionVO;
import com.projectexample.examsystem.vo.CurrentUserVO;
import com.projectexample.examsystem.vo.VerificationCodeSendVO;

import java.util.List;

public interface AuthService {

    AuthTokenVO login(LoginRequest request);

    CurrentUserVO currentUser(String username);

    List<AuthRegisterOptionVO> listRegisterOptions();

    VerificationCodeSendVO sendVerificationCode(SendVerificationCodeRequest request);

    void register(RegisterRequest request);

    void resetPassword(PasswordResetRequest request);
}
