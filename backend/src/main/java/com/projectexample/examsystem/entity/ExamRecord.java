package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_score_record")
@EqualsAndHashCode(callSuper = true)
public class ExamRecord extends BaseEntity {

    @TableField("exam_plan_id")
    private Long examPlanId;

    @TableField("answer_sheet_id")
    private Long answerSheetId;

    @TableField("user_id")
    private Long userId;

    private String candidateName;
    private String examName;
    private String paperName;

    private LocalDateTime submittedAt;

    private Double objectiveScore;
    private Double subjectiveScore;
    private Double finalScore;
    private Integer passedFlag;
    private Integer publishedFlag;
    private String status;
}
