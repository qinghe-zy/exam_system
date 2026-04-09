package com.projectexample.examsystem.dto;

import lombok.Data;

@Data
public class NotificationDeliveryLogQueryRequest {

    private long pageNum = 1;
    private long pageSize = 10;
    private String businessType;
    private String channelType;
    private String deliveryStatus;
    private Long recipientUserId;
}
