package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_exam_candidate")
@EqualsAndHashCode(callSuper = true)
public class ExamCandidate extends BaseEntity {

    private Long examPlanId;
    private Long userId;
    private String candidateName;
    private String organizationName;
    private String status;
    private String accessCode;
    private Integer attemptCount;
    private LocalDateTime assignedTime;
}
