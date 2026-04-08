package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_grading_record")
@EqualsAndHashCode(callSuper = true)
public class GradingRecord extends BaseEntity {

    private Long answerSheetId;
    private Long answerItemId;
    private Long graderId;
    private String graderName;
    private Integer reviewRound;
    private String gradingAction;
    private Double scoreAwarded;
    private String commentText;
    private LocalDateTime gradedAt;
    private String status;
}
