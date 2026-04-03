package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.KnowledgePointAutoGroupRequest;
import com.projectexample.examsystem.dto.QuestionImportRequest;
import com.projectexample.examsystem.dto.QuestionBankSaveRequest;
import com.projectexample.examsystem.service.QuestionBankService;
import com.projectexample.examsystem.vo.QuestionBankVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam/questions")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<QuestionBankVO>> list() { return ApiResponse.success(questionBankService.listQuestions()); }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<QuestionBankVO>> export() { return ApiResponse.success(questionBankService.exportQuestions()); }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<QuestionBankVO>> importQuestions(@Valid @RequestBody QuestionImportRequest request) { return ApiResponse.success("questions imported", questionBankService.importQuestions(request)); }

    @PostMapping("/auto-group/knowledge-points")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<QuestionBankVO>> autoGroupByKnowledgePoint(@Valid @RequestBody KnowledgePointAutoGroupRequest request) {
        return ApiResponse.success("knowledge point auto group generated", questionBankService.autoGroupByKnowledgePoint(request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<QuestionBankVO> create(@Valid @RequestBody QuestionBankSaveRequest request) { return ApiResponse.success("question created", questionBankService.createQuestion(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<QuestionBankVO> update(@PathVariable Long id, @Valid @RequestBody QuestionBankSaveRequest request) { return ApiResponse.success("question updated", questionBankService.updateQuestion(id, request)); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) { questionBankService.deleteQuestion(id); return ApiResponse.success("question deleted", null); }
}
