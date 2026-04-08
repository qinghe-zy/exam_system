package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GradingWorkspaceVO {

    private Long answerSheetId;
    private String examName;
    private String candidateName;
    private Double objectiveScore;
    private Double subjectiveScore;
    private Double finalScore;
    private String status;
    private String reviewStatus;
    private String appealStatus;
    private List<CandidateAnswerItemVO> items;
}
