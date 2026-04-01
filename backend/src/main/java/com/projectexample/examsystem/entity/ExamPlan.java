package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_exam_plan")
@EqualsAndHashCode(callSuper = true)
public class ExamPlan extends BaseEntity {

    private String examCode;
    private String examName;
    private Long organizationId;
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
    private Integer earlySubmitMinutes;
    private Integer autoSubmitEnabled;
    private String antiCheatLevel;
    private String instructionText;
    private Integer status;
    private Integer publishStatus;
}
