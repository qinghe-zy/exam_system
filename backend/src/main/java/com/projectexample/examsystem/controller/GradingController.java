package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.GradingReviewRequest;
import com.projectexample.examsystem.dto.GradingSubmitRequest;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.GradingService;
import com.projectexample.examsystem.vo.GradingTaskVO;
import com.projectexample.examsystem.vo.GradingWorkspaceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','GRADER','TEACHER')")
    public ApiResponse<List<GradingTaskVO>> tasks() {
        return ApiResponse.success(gradingService.listTasks());
    }

    @GetMapping("/{answerSheetId}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','GRADER','TEACHER')")
    public ApiResponse<GradingWorkspaceVO> workspace(@PathVariable Long answerSheetId) {
        return ApiResponse.success(gradingService.getWorkspace(answerSheetId));
    }

    @PostMapping("/{answerSheetId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','GRADER','TEACHER')")
    public ApiResponse<GradingWorkspaceVO> submit(@PathVariable Long answerSheetId,
                                                  @Valid @RequestBody GradingSubmitRequest request,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("grading updated", gradingService.submitGrading(answerSheetId, request, userPrincipal.getUsername()));
    }

    @PostMapping("/{answerSheetId}/review")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','GRADER','TEACHER')")
    public ApiResponse<GradingWorkspaceVO> review(@PathVariable Long answerSheetId,
                                                  @Valid @RequestBody GradingReviewRequest request,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("grading review updated", gradingService.reviewGrading(answerSheetId, request, userPrincipal.getUsername()));
    }
}
