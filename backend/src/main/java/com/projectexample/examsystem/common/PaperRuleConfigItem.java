package com.projectexample.examsystem.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperRuleConfigItem {

    private String code;
    private String label;
    private Integer count;
    private Double score;
}
