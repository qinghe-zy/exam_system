package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationTemplateVO {

    private Long id;
    private Long organizationId;
    private String templateCode;
    private String templateName;
    private String businessType;
    private String channelType;
    private String titleTemplate;
    private String contentTemplate;
    private Integer status;
    private LocalDateTime updateTime;
}
