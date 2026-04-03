package com.projectexample.examsystem.vo;

import com.projectexample.examsystem.common.PaperRuleConfigItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExamPaperVO {

    private Long id;
    private String paperCode;
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
    private List<PaperRuleConfigItem> questionTypeConfigs;
    private List<PaperRuleConfigItem> difficultyConfigs;
    private Integer publishStatus;
    private List<PaperQuestionItemVO> questionItems;
}
