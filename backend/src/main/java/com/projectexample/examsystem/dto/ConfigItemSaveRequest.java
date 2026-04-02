package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfigItemSaveRequest {

    @NotBlank(message = "Configuration key is required")
    private String configKey;

    @NotBlank(message = "Configuration name is required")
    private String configName;

    @NotBlank(message = "Configuration group is required")
    private String configGroup;

    @NotBlank(message = "Configuration value is required")
    private String configValue;

    private String descriptionText;

    @NotNull(message = "Status is required")
    private Integer status;
}
