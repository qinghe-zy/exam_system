package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.service.AnalyticsService;
import com.projectexample.examsystem.vo.AnalysisOverviewVO;
import com.projectexample.examsystem.vo.AnalysisQualityReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<AnalysisOverviewVO> overview() {
        return ApiResponse.success(analyticsService.getOverview());
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analysis-overview.csv")
                .contentType(new MediaType("text", "csv"))
                .body(analyticsService.exportOverviewCsv());
    }

    @GetMapping("/quality-report")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<AnalysisQualityReportVO> qualityReport() {
        return ApiResponse.success(analyticsService.getQualityReport());
    }

    @GetMapping(value = "/quality-report/export", produces = "text/markdown;charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ResponseEntity<String> exportQualityReport() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exam-quality-report.md")
                .contentType(new MediaType("text", "markdown"))
                .body(analyticsService.exportQualityReportMarkdown());
    }
}
