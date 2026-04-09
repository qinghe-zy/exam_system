package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateExamVO {

    private Long examPlanId;
    private String examName;
    private String examMode;
    private String batchLabel;
    private String examRoom;
    private Long sourceExamPlanId;
    private String sourceExamName;
    private String seatNo;
    private String paperName;
    private String subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime entryDeadlineAt;
    private LocalDateTime signInOpenAt;
    private LocalDateTime signInDeadlineAt;
    private LocalDateTime signedInAt;
    private LocalDateTime answerDeadlineAt;
    private Integer durationMinutes;
    private Integer signInRequired;
    private Integer signedInFlag;
    private String candidateStatus;
    private Integer attemptCount;
    private String answerSheetStatus;
}
