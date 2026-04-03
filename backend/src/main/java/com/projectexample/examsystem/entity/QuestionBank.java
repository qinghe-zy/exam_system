package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_question_bank")
@EqualsAndHashCode(callSuper = true)
public class QuestionBank extends BaseEntity {

    private String questionCode;
    private Long organizationId;
    private String subject;
    private String questionType;
    private String difficultyLevel;
    private String stem;
    private String stemHtml;
    private String materialContent;
    private String attachmentJson;
    private String optionsJson;
    private String answerKey;
    private String analysisText;
    private String knowledgePoint;
    private String chapterName;
    private String sourceName;
    private String tags;
    private Double defaultScore;
    private String reviewerStatus;
    private Integer versionNo;
    private Integer status;
}
