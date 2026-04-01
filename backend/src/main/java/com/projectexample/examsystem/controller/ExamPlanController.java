package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.ExamPlanSaveRequest;
import com.projectexample.examsystem.service.ExamPlanService;
import com.projectexample.examsystem.vo.ExamPlanVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam/plans")
@RequiredArgsConstructor
public class ExamPlanController {

    private final ExamPlanService examPlanService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<ExamPlanVO>> list() {
        return ApiResponse.success(examPlanService.listPlans());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<ExamPlanVO> create(@Valid @RequestBody ExamPlanSaveRequest request) {
        return ApiResponse.success("exam plan created", examPlanService.createPlan(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<ExamPlanVO> update(@PathVariable Long id, @Valid @RequestBody ExamPlanSaveRequest request) {
        return ApiResponse.success("exam plan updated", examPlanService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examPlanService.deletePlan(id);
        return ApiResponse.success("exam plan deleted", null);
    }
}
