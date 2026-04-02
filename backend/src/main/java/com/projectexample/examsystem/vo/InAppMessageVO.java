package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InAppMessageVO {

    private Long id;
    private String title;
    private String messageType;
    private String content;
    private String relatedType;
    private Long relatedId;
    private Integer readFlag;
    private LocalDateTime createTime;
}
