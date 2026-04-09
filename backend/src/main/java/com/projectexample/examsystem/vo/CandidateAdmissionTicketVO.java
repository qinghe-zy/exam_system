package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateAdmissionTicketVO {

    private Long examPlanId;
    private String examCode;
    private String examName;
    private String examMode;
    private String batchLabel;
    private String examRoom;
    private String sourceExamName;
    private String paperName;
    private String subject;
    private String candidateName;
    private String organizationName;
    private String accessCode;
    private String seatNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime entryDeadlineAt;
    private Integer durationMinutes;
    private Integer signInRequired;
    private LocalDateTime signInOpenAt;
    private LocalDateTime signInDeadlineAt;
    private Integer signedInFlag;
    private LocalDateTime signedInAt;
    private String instructionText;
}
