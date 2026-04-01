package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NoticeVO {

    private Long id;
    private String title;
    private String category;
    private Integer status;
    private String content;
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
}
