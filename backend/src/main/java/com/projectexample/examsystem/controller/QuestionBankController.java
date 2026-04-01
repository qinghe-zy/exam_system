package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.QuestionBankSaveRequest;
import com.projectexample.examsystem.service.QuestionBankService;
import com.projectexample.examsystem.vo.QuestionBankVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam/questions")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping
    public ApiResponse<List<QuestionBankVO>> list() { return ApiResponse.success(questionBankService.listQuestions()); }

    @PostMapping
    public ApiResponse<QuestionBankVO> create(@Valid @RequestBody QuestionBankSaveRequest request) { return ApiResponse.success("question created", questionBankService.createQuestion(request)); }

    @PutMapping("/{id}")
    public ApiResponse<QuestionBankVO> update(@PathVariable Long id, @Valid @RequestBody QuestionBankSaveRequest request) { return ApiResponse.success("question updated", questionBankService.updateQuestion(id, request)); }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) { questionBankService.deleteQuestion(id); return ApiResponse.success("question deleted", null); }
}
