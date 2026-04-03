package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateExamVO {

    private Long examPlanId;
    private String examName;
    private String paperName;
    private String subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime entryDeadlineAt;
    private LocalDateTime answerDeadlineAt;
    private Integer durationMinutes;
    private String candidateStatus;
    private Integer attemptCount;
    private String answerSheetStatus;
}
