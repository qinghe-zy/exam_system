package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.GradingSubmitRequest;
import com.projectexample.examsystem.dto.GradingReviewRequest;
import com.projectexample.examsystem.vo.GradingTaskVO;
import com.projectexample.examsystem.vo.GradingWorkspaceVO;

import java.util.List;

public interface GradingService {

    List<GradingTaskVO> listTasks();

    GradingWorkspaceVO getWorkspace(Long answerSheetId);

    GradingWorkspaceVO submitGrading(Long answerSheetId, GradingSubmitRequest request, String username);

    GradingWorkspaceVO reviewGrading(Long answerSheetId, GradingReviewRequest request, String username);
}
