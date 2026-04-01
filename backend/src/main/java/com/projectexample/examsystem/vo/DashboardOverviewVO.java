package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardOverviewVO {

    private String headline;
    private String summary;
    private List<DashboardMetricVO> metrics;
    private List<String> nextActions;
}
