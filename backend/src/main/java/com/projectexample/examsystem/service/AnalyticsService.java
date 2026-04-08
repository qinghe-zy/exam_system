package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.AnalysisOverviewVO;
import com.projectexample.examsystem.vo.AnalysisQualityReportVO;

public interface AnalyticsService {

    AnalysisOverviewVO getOverview();

    String exportOverviewCsv();

    AnalysisQualityReportVO getQualityReport();

    String exportQualityReportMarkdown();
}
