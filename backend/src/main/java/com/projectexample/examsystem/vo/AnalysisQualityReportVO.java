package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AnalysisQualityReportVO {

    private LocalDateTime generatedAt;
    private Double overallQualityScore;
    private String overallQualityLevel;
    private String summary;
    private String riskSummary;
    private List<String> recommendations;
    private List<QualityDimensionVO> dimensionScores;
    private List<ExamQualityInsightVO> examInsights;
    private List<KnowledgePointAnalysisVO> weakKnowledgePoints;
    private List<QuestionScoreRateVO> weakQuestions;
}
