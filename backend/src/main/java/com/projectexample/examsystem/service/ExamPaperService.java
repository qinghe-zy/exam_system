package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.ExamPaperSaveRequest;
import com.projectexample.examsystem.vo.ExamPaperVO;

import java.util.List;

public interface ExamPaperService {

    List<ExamPaperVO> listPapers();

    ExamPaperVO getPaper(Long id);

    ExamPaperVO createPaper(ExamPaperSaveRequest request);

    ExamPaperVO updatePaper(Long id, ExamPaperSaveRequest request);

    void deletePaper(Long id);
}
