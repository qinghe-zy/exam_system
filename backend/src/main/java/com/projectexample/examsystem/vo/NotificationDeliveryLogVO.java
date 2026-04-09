package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDeliveryLogVO {

    private Long id;
    private Long organizationId;
    private String businessType;
    private String channelType;
    private String templateCode;
    private Long recipientUserId;
    private String recipientName;
    private String recipientTarget;
    private String title;
    private String content;
    private String relatedType;
    private Long relatedId;
    private String businessKey;
    private String deliveryStatus;
    private String providerTrace;
    private LocalDateTime sentAt;
}
