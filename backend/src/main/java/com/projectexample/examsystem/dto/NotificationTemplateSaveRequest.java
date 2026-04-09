package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationTemplateSaveRequest {

    @NotBlank(message = "Template code is required")
    private String templateCode;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotBlank(message = "Channel type is required")
    private String channelType;

    @NotBlank(message = "Title template is required")
    private String titleTemplate;

    @NotBlank(message = "Content template is required")
    private String contentTemplate;

    @NotNull(message = "Status is required")
    private Integer status;
}
