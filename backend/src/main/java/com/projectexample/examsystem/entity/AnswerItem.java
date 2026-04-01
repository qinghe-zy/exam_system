package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_answer_item")
@EqualsAndHashCode(callSuper = true)
public class AnswerItem extends BaseEntity {

    private Long answerSheetId;
    private Long questionId;
    private String questionType;
    private Integer questionOrder;
    private String answerContent;
    private Double maxScore;
    private Double scoreAwarded;
    private Integer autoScored;
    private Integer markedFlag;
    private String reviewComment;
    private String status;
}
