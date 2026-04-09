package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("biz_notification_delivery_log")
@EqualsAndHashCode(callSuper = true)
public class NotificationDeliveryLog extends BaseEntity {

    @TableField("organization_id")
    private Long organizationId;

    @TableField("business_type")
    private String businessType;

    @TableField("channel_type")
    private String channelType;

    @TableField("template_code")
    private String templateCode;

    @TableField("recipient_user_id")
    private Long recipientUserId;

    @TableField("recipient_name")
    private String recipientName;

    @TableField("recipient_target")
    private String recipientTarget;

    private String title;
    private String content;

    @TableField("related_type")
    private String relatedType;

    @TableField("related_id")
    private Long relatedId;

    @TableField("business_key")
    private String businessKey;

    @TableField("delivery_status")
    private String deliveryStatus;

    @TableField("provider_trace")
    private String providerTrace;

    @TableField("sent_at")
    private LocalDateTime sentAt;
}
