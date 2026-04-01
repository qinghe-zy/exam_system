package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_paper_question")
@EqualsAndHashCode(callSuper = true)
public class PaperQuestion extends BaseEntity {

    private Long paperId;
    private Long questionId;
    private Integer sortNo;
    private Double score;
    private Integer requiredFlag;
}
