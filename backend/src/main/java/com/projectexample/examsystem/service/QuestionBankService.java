package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.QuestionBankSaveRequest;
import com.projectexample.examsystem.vo.QuestionBankVO;

import java.util.List;

public interface QuestionBankService {

    List<QuestionBankVO> listQuestions();

    QuestionBankVO createQuestion(QuestionBankSaveRequest request);

    QuestionBankVO updateQuestion(Long id, QuestionBankSaveRequest request);

    void deleteQuestion(Long id);
}
