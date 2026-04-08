package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.LoginRequest;
import com.projectexample.examsystem.dto.PasswordResetRequest;
import com.projectexample.examsystem.dto.RegisterRequest;
import com.projectexample.examsystem.dto.SendVerificationCodeRequest;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.AuthService;
import com.projectexample.examsystem.vo.AuthTokenVO;
import com.projectexample.examsystem.vo.AuthRegisterOptionVO;
import com.projectexample.examsystem.vo.CurrentUserVO;
import com.projectexample.examsystem.vo.VerificationCodeSendVO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthTokenVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authService.login(
                request,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent"),
                httpServletRequest.getHeader("X-Device-Fingerprint"),
                httpServletRequest.getHeader("X-Device-Info")
        ));
    }

    @GetMapping("/register-options")
    public ApiResponse<List<AuthRegisterOptionVO>> registerOptions() {
        return ApiResponse.success(authService.listRegisterOptions());
    }

    @PostMapping("/verification-codes/send")
    public ApiResponse<VerificationCodeSendVO> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        return ApiResponse.success("verification code sent", authService.sendVerificationCode(request));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("register success", null);
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("password reset success", null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> currentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(authService.currentUser(userPrincipal.getUsername()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal != null) {
            authService.logout(userPrincipal.getUsername());
        }
        return ApiResponse.success("logout success", null);
    }
}
