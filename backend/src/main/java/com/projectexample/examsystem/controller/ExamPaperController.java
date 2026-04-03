package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.ExamPaperSaveRequest;
import com.projectexample.examsystem.service.ExamPaperService;
import com.projectexample.examsystem.vo.ExamPaperVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam/papers")
@RequiredArgsConstructor
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<ExamPaperVO>> list() { return ApiResponse.success(examPaperService.listPapers()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<ExamPaperVO> detail(@PathVariable Long id) { return ApiResponse.success(examPaperService.getPaper(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<ExamPaperVO> create(@Valid @RequestBody ExamPaperSaveRequest request) { return ApiResponse.success("paper created", examPaperService.createPaper(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<ExamPaperVO> update(@PathVariable Long id, @Valid @RequestBody ExamPaperSaveRequest request) { return ApiResponse.success("paper updated", examPaperService.updatePaper(id, request)); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) { examPaperService.deletePaper(id); return ApiResponse.success("paper deleted", null); }
}
