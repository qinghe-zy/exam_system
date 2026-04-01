package com.projectexample.examsystem.vo;

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
    private Integer durationMinutes;
    private Double totalScore;
    private Double passScore;
    private Integer questionCount;
    private Integer publishStatus;
    private List<PaperQuestionItemVO> questionItems;
}
