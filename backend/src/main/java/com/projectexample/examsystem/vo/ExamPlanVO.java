package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamPlanVO {

    private Long id;
    private String examCode;
    private String examName;
    private String examMode;
    private String batchLabel;
    private String examRoom;
    private Long sourceExamPlanId;
    private String sourceExamName;
    private Long paperId;
    private String paperName;
    private String subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Double passScore;
    private String candidateScope;
    private Integer attemptLimit;
    private String examPassword;
    private Integer lateEntryMinutes;
    private Integer signInRequired;
    private Integer signInStartMinutes;
    private Integer earlySubmitMinutes;
    private Integer autoSubmitEnabled;
    private String antiCheatLevel;
    private String instructionText;
    private Integer status;
    private Integer publishStatus;
    private Integer candidateCount;
    private Integer signedInCount;
    private Double signInRate;
    private Integer submittedCount;
    private List<Long> candidateUserIds;
}
