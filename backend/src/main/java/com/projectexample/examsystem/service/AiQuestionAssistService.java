package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.AiQuestionDraftRequest;
import com.projectexample.examsystem.dto.AiQuestionPolishRequest;
import com.projectexample.examsystem.vo.AiQuestionDraftVO;
import com.projectexample.examsystem.vo.AiQuestionPolishVO;

public interface AiQuestionAssistService {

    AiQuestionDraftVO generateDraft(AiQuestionDraftRequest request);

    AiQuestionPolishVO polishQuestion(AiQuestionPolishRequest request);
}
