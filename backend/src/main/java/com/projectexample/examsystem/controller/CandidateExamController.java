package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.CandidateAnswerSheetSaveRequest;
import com.projectexample.examsystem.dto.CandidateEventReportRequest;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.CandidateExamService;
import com.projectexample.examsystem.vo.CandidateAdmissionTicketVO;
import com.projectexample.examsystem.vo.CandidateExamVO;
import com.projectexample.examsystem.vo.CandidateExamWorkspaceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/exam/candidate")
@RequiredArgsConstructor
public class CandidateExamController {

    private final CandidateExamService candidateExamService;

    @GetMapping("/my-exams")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<CandidateExamVO>> myExams(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(candidateExamService.listMyExams(userPrincipal.getUsername()));
    }

    @GetMapping("/exams/{examPlanId}/admission-ticket")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateAdmissionTicketVO> admissionTicket(@PathVariable Long examPlanId,
                                                                   @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(candidateExamService.getAdmissionTicket(examPlanId, userPrincipal.getUsername()));
    }

    @PostMapping("/exams/{examPlanId}/sign-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateExamVO> signIn(@PathVariable Long examPlanId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("exam signed in", candidateExamService.signIn(examPlanId, userPrincipal.getUsername()));
    }

    @GetMapping("/exams/{examPlanId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateExamWorkspaceVO> workspace(@PathVariable Long examPlanId,
                                                           @RequestParam(required = false) String examPassword,
                                                           @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                           HttpServletRequest httpServletRequest) {
        return ApiResponse.success(candidateExamService.getWorkspace(
                examPlanId,
                examPassword,
                userPrincipal.getUsername(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("X-Device-Fingerprint"),
                httpServletRequest.getHeader("X-Device-Info")
        ));
    }

    @PostMapping("/exams/{examPlanId}/save")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateExamWorkspaceVO> save(@PathVariable Long examPlanId,
                                                      @Valid @RequestBody CandidateAnswerSheetSaveRequest request,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("answers saved", candidateExamService.saveAnswers(examPlanId, request, false, userPrincipal.getUsername()));
    }

    @PostMapping("/exams/{examPlanId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CandidateExamWorkspaceVO> submit(@PathVariable Long examPlanId,
                                                        @Valid @RequestBody CandidateAnswerSheetSaveRequest request,
                                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success("exam submitted", candidateExamService.saveAnswers(examPlanId, request, true, userPrincipal.getUsername()));
    }

    @PostMapping("/exams/{examPlanId}/events")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> reportEvent(@PathVariable Long examPlanId,
                                         @Valid @RequestBody CandidateEventReportRequest request,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal,
                                         HttpServletRequest httpServletRequest) {
        candidateExamService.reportEvent(examPlanId, request, userPrincipal.getUsername(), httpServletRequest.getRemoteAddr());
        return ApiResponse.success("event recorded", null);
    }
}
