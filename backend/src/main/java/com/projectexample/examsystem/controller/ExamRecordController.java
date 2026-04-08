package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.ExamRecordService;
import com.projectexample.examsystem.vo.CandidateScoreDetailVO;
import com.projectexample.examsystem.vo.CandidateWrongQuestionVO;
import com.projectexample.examsystem.vo.ExamRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER','GRADER')")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=score-records.csv")
                .contentType(new MediaType("text", "csv"))
                .body(examRecordService.exportRecordsCsv());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ExamRecordVO>> myRecords(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(examRecordService.listMyRecords(userPrincipal.getUsername()));
    }

    @GetMapping("/my/wrong-book")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<CandidateWrongQuestionVO>> myWrongQuestions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(examRecordService.listMyWrongQuestions(userPrincipal.getUsername()));
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateScoreDetailVO> myRecordDetail(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(examRecordService.getMyRecordDetail(id, userPrincipal.getUsername()));
    }
}
