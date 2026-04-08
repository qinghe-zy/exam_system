package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalysisOverviewVO {

    private Long totalExamPlans;
    private Long totalAnswerSheets;
    private Double averageScore;
    private Double passRate;
    private Double excellentRate;
    private List<ExamPerformanceVO> examPerformances;
    private List<OrganizationComparisonVO> organizationComparisons;
    private List<TrendPointVO> trendPoints;
    private List<RankingVO> rankings;
    private List<ScoreBandVO> scoreBands;
    private List<KnowledgePointAnalysisVO> knowledgePoints;
    private List<QuestionScoreRateVO> questionScoreRates;
}
