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
    private LocalDateTime entryDeadlineAt;
    private LocalDateTime answerDeadlineAt;
    private Integer durationMinutes;
    private Integer remainingSeconds;
    private Long answerSheetId;
    private String answerSheetStatus;
    private Integer autoSubmitEnabled;
    private Integer autoSubmitFlag;
    private Integer saveVersion;
    private Integer shuffleEnabled;
    private String paperVersion;
    private List<CandidateAnswerItemVO> items;
}
