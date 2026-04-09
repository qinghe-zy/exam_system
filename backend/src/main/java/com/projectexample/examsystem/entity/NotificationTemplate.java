package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_notification_template")
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends BaseEntity {

    @TableField("organization_id")
    private Long organizationId;

    @TableField("template_code")
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("business_type")
    private String businessType;

    @TableField("channel_type")
    private String channelType;

    @TableField("title_template")
    private String titleTemplate;

    @TableField("content_template")
    private String contentTemplate;

    private Integer status;
}
