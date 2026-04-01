package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidateExamWorkspaceVO {

    private Long examPlanId;
    private String examName;
    private String paperName;
    private String subject;
    private String instructionText;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Long answerSheetId;
    private String answerSheetStatus;
    private List<CandidateAnswerItemVO> items;
}
