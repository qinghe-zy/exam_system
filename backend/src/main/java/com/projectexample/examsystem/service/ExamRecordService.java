package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.CandidateScoreDetailVO;
import com.projectexample.examsystem.vo.ExamRecordVO;

import java.util.List;

public interface ExamRecordService {

    List<ExamRecordVO> listRecords();

    List<ExamRecordVO> listMyRecords(String username);

    CandidateScoreDetailVO getMyRecordDetail(Long recordId, String username);
}
