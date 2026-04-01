package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_audit_log")
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    private Long operatorId;
    private String operatorName;
    private String moduleName;
    private String actionName;
    private String targetType;
    private Long targetId;
    private String detailText;
}
