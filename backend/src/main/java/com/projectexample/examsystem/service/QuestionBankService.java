package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.QuestionImportRequest;
import com.projectexample.examsystem.dto.QuestionBankSaveRequest;
import com.projectexample.examsystem.vo.QuestionBankVO;

import java.util.List;

public interface QuestionBankService {

    List<QuestionBankVO> listQuestions();

    List<QuestionBankVO> exportQuestions();

    List<QuestionBankVO> importQuestions(QuestionImportRequest request);

    QuestionBankVO createQuestion(QuestionBankSaveRequest request);

    QuestionBankVO updateQuestion(Long id, QuestionBankSaveRequest request);

    void deleteQuestion(Long id);
}
