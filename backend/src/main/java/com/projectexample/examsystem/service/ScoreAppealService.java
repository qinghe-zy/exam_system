package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.ScoreAppealProcessRequest;
import com.projectexample.examsystem.dto.ScoreAppealSubmitRequest;
import com.projectexample.examsystem.vo.ScoreAppealVO;

import java.util.List;

public interface ScoreAppealService {

    List<ScoreAppealVO> listAppeals(Long scoreRecordId);

    List<ScoreAppealVO> listMyAppeals(Long scoreRecordId, String username);

    ScoreAppealVO submitAppeal(Long scoreRecordId, ScoreAppealSubmitRequest request, String username);

    ScoreAppealVO processAppeal(Long appealId, ScoreAppealProcessRequest request, String username);
}
