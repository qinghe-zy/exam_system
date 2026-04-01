package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.service.AntiCheatService;
import com.projectexample.examsystem.vo.AntiCheatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam/proctor")
@RequiredArgsConstructor
public class ProctorController {

    private final AntiCheatService antiCheatService;

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','PROCTOR')")
    public ApiResponse<List<AntiCheatEventVO>> events() {
        return ApiResponse.success(antiCheatService.listEvents());
    }
}
