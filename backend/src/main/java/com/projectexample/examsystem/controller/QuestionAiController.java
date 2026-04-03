package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.AiQuestionDraftRequest;
import com.projectexample.examsystem.dto.AiQuestionPolishRequest;
import com.projectexample.examsystem.service.AiQuestionAssistService;
import com.projectexample.examsystem.vo.AiQuestionDraftVO;
import com.projectexample.examsystem.vo.AiQuestionPolishVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam/questions/ai")
@RequiredArgsConstructor
public class QuestionAiController {

    private final AiQuestionAssistService aiQuestionAssistService;

    @PostMapping("/draft")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<AiQuestionDraftVO> generateDraft(@Valid @RequestBody AiQuestionDraftRequest request) {
        return ApiResponse.success("ai draft generated", aiQuestionAssistService.generateDraft(request));
    }

    @PostMapping("/polish")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<AiQuestionPolishVO> polish(@Valid @RequestBody AiQuestionPolishRequest request) {
        return ApiResponse.success("ai polish generated", aiQuestionAssistService.polishQuestion(request));
    }
}
