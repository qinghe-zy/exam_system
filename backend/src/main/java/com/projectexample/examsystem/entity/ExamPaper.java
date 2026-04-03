package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_exam_paper")
@EqualsAndHashCode(callSuper = true)
public class ExamPaper extends BaseEntity {

    private String paperCode;
    private Long organizationId;
    private String paperName;
    private String subject;
    private String assemblyMode;
    private String descriptionText;
    private String paperVersion;
    private String remarkText;
    private Integer durationMinutes;
    private Double totalScore;
    private Double passScore;
    private Integer questionCount;
    private Integer shuffleEnabled;
    private String questionTypeConfigJson;
    private String difficultyConfigJson;
    private Integer publishStatus;
}
