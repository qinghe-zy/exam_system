package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_answer_sheet")
@EqualsAndHashCode(callSuper = true)
public class AnswerSheet extends BaseEntity {

    private Long examPlanId;
    private Long paperId;
    private String paperName;
    private Long userId;
    private String candidateName;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String status;
    private Double objectiveScore;
    private Double subjectiveScore;
    private Double finalScore;
    private Integer autoSubmitFlag;
    private Integer saveVersion;
}
