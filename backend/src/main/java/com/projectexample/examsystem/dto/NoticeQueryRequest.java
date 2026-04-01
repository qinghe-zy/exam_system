package com.projectexample.examsystem.dto;

import lombok.Data;

@Data
public class NoticeQueryRequest {

    private long pageNum = 1;
    private long pageSize = 10;
    private String title;
    private Integer status;
}
