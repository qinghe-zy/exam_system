package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogVO {

    private Long id;
    private String operatorName;
    private String moduleName;
    private String actionName;
    private String targetType;
    private Long targetId;
    private String detailText;
    private LocalDateTime createTime;
}
