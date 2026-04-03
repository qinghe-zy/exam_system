package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.ExamRecordService;
import com.projectexample.examsystem.vo.CandidateScoreDetailVO;
import com.projectexample.examsystem.vo.ExamRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam/records")
@RequiredArgsConstructor
public class ExamRecordController {

    private final ExamRecordService examRecordService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER','GRADER')")
    public ApiResponse<List<ExamRecordVO>> list() {
        return ApiResponse.success(examRecordService.listRecords());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ExamRecordVO>> myRecords(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(examRecordService.listMyRecords(userPrincipal.getUsername()));
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateScoreDetailVO> myRecordDetail(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(examRecordService.getMyRecordDetail(id, userPrincipal.getUsername()));
    }
}
