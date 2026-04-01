package com.projectexample.examsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardMetricVO {

    private String label;
    private Long value;
    private String description;
}
