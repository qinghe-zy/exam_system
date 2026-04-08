package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.service.LoginRiskService;
import com.projectexample.examsystem.vo.LoginRiskLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/login-risks")
@RequiredArgsConstructor
public class LoginRiskController {

    private final LoginRiskService loginRiskService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<List<LoginRiskLogVO>> list() {
        return ApiResponse.success(loginRiskService.listLogs());
    }
}
