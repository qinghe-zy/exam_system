package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.ExamPlanSaveRequest;
import com.projectexample.examsystem.vo.ExamPlanVO;

import java.util.List;

public interface ExamPlanService {

    List<ExamPlanVO> listPlans();

    ExamPlanVO createPlan(ExamPlanSaveRequest request);

    ExamPlanVO updatePlan(Long id, ExamPlanSaveRequest request);

    void deletePlan(Long id);

    String exportSignInSheetCsv(Long id);
}
