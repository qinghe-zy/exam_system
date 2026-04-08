package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_score_appeal")
@EqualsAndHashCode(callSuper = true)
public class ScoreAppeal extends BaseEntity {

    @TableField("score_record_id")
    private Long scoreRecordId;

    @TableField("answer_sheet_id")
    private Long answerSheetId;

    @TableField("exam_plan_id")
    private Long examPlanId;

    @TableField("user_id")
    private Long userId;

    private String candidateName;
    private String examName;
    private String appealReason;
    private String expectedOutcome;
    private String status;
    private String resolutionAction;
    private String processComment;
    private Long processedBy;
    private String processedByName;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
}
