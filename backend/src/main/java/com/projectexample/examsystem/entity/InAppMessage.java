package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_in_app_message")
@EqualsAndHashCode(callSuper = true)
public class InAppMessage extends BaseEntity {

    @TableField("recipient_user_id")
    private Long recipientUserId;

    private String title;
    private String messageType;
    private String content;
    private String relatedType;
    private Long relatedId;
    private Integer readFlag;
}
