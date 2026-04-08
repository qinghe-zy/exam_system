package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.CandidateScoreDetailVO;
import com.projectexample.examsystem.vo.CandidateWrongQuestionVO;
import com.projectexample.examsystem.vo.ExamRecordVO;

import java.util.List;

public interface ExamRecordService {

    List<ExamRecordVO> listRecords();

    String exportRecordsCsv();

    List<ExamRecordVO> listMyRecords(String username);

    CandidateScoreDetailVO getMyRecordDetail(Long recordId, String username);

    List<CandidateWrongQuestionVO> listMyWrongQuestions(String username);
}
