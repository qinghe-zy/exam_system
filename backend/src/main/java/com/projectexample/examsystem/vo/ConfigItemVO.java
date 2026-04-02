package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigItemVO {

    private Long id;
    private String configKey;
    private String configName;
    private String configGroup;
    private String configValue;
    private String descriptionText;
    private Integer status;
}
