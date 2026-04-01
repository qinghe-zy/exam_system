package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.CandidateAnswerSheetSaveRequest;
import com.projectexample.examsystem.dto.CandidateEventReportRequest;
import com.projectexample.examsystem.vo.CandidateExamVO;
import com.projectexample.examsystem.vo.CandidateExamWorkspaceVO;

import java.util.List;

public interface CandidateExamService {

    List<CandidateExamVO> listMyExams(String username);

    CandidateExamWorkspaceVO getWorkspace(Long examPlanId, String username);

    CandidateExamWorkspaceVO saveAnswers(Long examPlanId, CandidateAnswerSheetSaveRequest request, boolean submit, String username);

    void reportEvent(Long examPlanId, CandidateEventReportRequest request, String username);
}
