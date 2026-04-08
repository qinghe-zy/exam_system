package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.ScoreAppealProcessRequest;
import com.projectexample.examsystem.dto.ScoreAppealSubmitRequest;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.ScoreAppealService;
import com.projectexample.examsystem.vo.ScoreAppealVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam/score-appeals")
@RequiredArgsConstructor
public class ScoreAppealController {

    private final ScoreAppealService scoreAppealService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','GRADER','TEACHER')")
    public ApiResponse<List<ScoreAppealVO>> list(@RequestParam(required = false) Long scoreRecordId) {
        return ApiResponse.success(scoreAppealService.listAppeals(scoreRecordId));
    }

    @GetMapping("/my/{scoreRecordId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ScoreAppealVO>> myAppeals(@PathVariable Long scoreRecordId,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(scoreAppealService.listMyAppeals(scoreRecordId, userPrincipal.getUsername()));
    }

    @PostMapping("/my/{scoreRecordId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ScoreAppealVO> submit(@PathVariable Long scoreRecordId,
                                             @Valid @RequestBody ScoreAppealSubmitRequest request,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("score appeal submitted", scoreAppealService.submitAppeal(scoreRecordId, request, userPrincipal.getUsername()));
    }

    @PostMapping("/{appealId}/process")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','GRADER','TEACHER')")
    public ApiResponse<ScoreAppealVO> process(@PathVariable Long appealId,
                                              @Valid @RequestBody ScoreAppealProcessRequest request,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("score appeal processed", scoreAppealService.processAppeal(appealId, request, userPrincipal.getUsername()));
    }
}
