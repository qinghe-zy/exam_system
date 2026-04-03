package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperQuestionItemRequest {

    @NotNull(message = "题目不能为空")
    private Long questionId;

    @NotNull(message = "题目顺序不能为空")
    private Integer sortNo;

    @NotNull(message = "题目分值不能为空")
    private Double score;

    @NotNull(message = "必答标记不能为空")
    private Integer requiredFlag;
}
