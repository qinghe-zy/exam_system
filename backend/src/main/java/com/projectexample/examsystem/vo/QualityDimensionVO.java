package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualityDimensionVO {

    private String dimensionName;
    private Double score;
    private String level;
    private String summary;
}
