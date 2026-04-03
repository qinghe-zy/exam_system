package com.projectexample.examsystem.dto;

import com.projectexample.examsystem.common.PaperRuleConfigItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamPaperSaveRequest {

    @NotBlank(message = "试卷编码不能为空")
    private String paperCode;

    @NotBlank(message = "试卷名称不能为空")
    private String paperName;

    @NotBlank(message = "学科不能为空")
    private String subject;

    @NotBlank(message = "组卷方式不能为空")
    private String assemblyMode;

    private String descriptionText;
    private String paperVersion;
    private String remarkText;

    @NotNull(message = "考试时长不能为空")
    private Integer durationMinutes;

    @NotNull(message = "总分不能为空")
    private Double totalScore;

    @NotNull(message = "及格线不能为空")
    private Double passScore;

    @NotNull(message = "乱序设置不能为空")
    private Integer shuffleEnabled;

    @Valid
    private List<PaperRuleConfigItem> questionTypeConfigs;

    @Valid
    private List<PaperRuleConfigItem> difficultyConfigs;

    @NotNull(message = "发布状态不能为空")
    private Integer publishStatus;

    @NotEmpty(message = "至少需要选择一道题目")
    @Valid
    private List<PaperQuestionItemRequest> questionItems;
}
