package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidateScoreDetailVO {

    private Long id;
    private Long examPlanId;
    private Long answerSheetId;
    private String examName;
    private String paperName;
    private String candidateName;
    private LocalDateTime submittedAt;
    private Double objectiveScore;
    private Double subjectiveScore;
    private Double finalScore;
    private Integer passedFlag;
    private Integer publishedFlag;
    private String status;
    private List<CandidateScoreItemVO> items;
}
