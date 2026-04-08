package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.AnalyticsService;
import com.projectexample.examsystem.vo.AnalysisOverviewVO;
import com.projectexample.examsystem.vo.AnalysisQualityReportVO;
import com.projectexample.examsystem.vo.ExamPerformanceVO;
import com.projectexample.examsystem.vo.ExamQualityInsightVO;
import com.projectexample.examsystem.vo.KnowledgePointAnalysisVO;
import com.projectexample.examsystem.vo.OrganizationComparisonVO;
import com.projectexample.examsystem.vo.QualityDimensionVO;
import com.projectexample.examsystem.vo.QuestionScoreRateVO;
import com.projectexample.examsystem.vo.RankingVO;
import com.projectexample.examsystem.vo.ScoreBandVO;
import com.projectexample.examsystem.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter REPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerItemMapper answerItemMapper;
    private final QuestionBankMapper questionBankMapper;
    private final SysUserMapper sysUserMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public AnalysisOverviewVO getOverview() {
        AnalyticsContext context = loadContext();
        List<ExamPerformanceVO> performances = context.plans().stream().map(plan -> {
            List<ExamRecord> currentRecords = context.records().stream()
                    .filter(record -> plan.getId().equals(record.getExamPlanId()))
                    .toList();
            int candidateCount = countAnswerSheets(plan.getId(), context.answerSheets());
            int submittedCount = currentRecords.size();
            int gradedCount = (int) currentRecords.stream().filter(record -> "PUBLISHED".equals(record.getStatus())).count();
            double average = currentRecords.stream().map(ExamRecord::getFinalScore).mapToDouble(this::value).average().orElse(0D);
            double highest = currentRecords.stream().map(ExamRecord::getFinalScore).mapToDouble(this::value).max().orElse(0D);
            double lowest = currentRecords.stream().map(ExamRecord::getFinalScore).mapToDouble(this::value).min().orElse(0D);
            double passRate = currentRecords.isEmpty() ? 0D : currentRecords.stream()
                    .filter(record -> record.getPassedFlag() != null && record.getPassedFlag() == 1)
                    .count() * 100D / currentRecords.size();
            double excellentRate = currentRecords.isEmpty() ? 0D : currentRecords.stream()
                    .filter(record -> value(record.getFinalScore()) >= 90D)
                    .count() * 100D / currentRecords.size();
            return ExamPerformanceVO.builder()
                    .examPlanId(plan.getId())
                    .examName(plan.getExamName())
                    .candidateCount(candidateCount)
                    .submittedCount(submittedCount)
                    .gradedCount(gradedCount)
                    .averageScore(round(average))
                    .highestScore(round(highest))
                    .lowestScore(round(lowest))
                    .passRate(round(passRate))
                    .excellentRate(round(excellentRate))
                    .build();
        }).toList();

        double overallAverage = context.records().stream().map(ExamRecord::getFinalScore).mapToDouble(this::value).average().orElse(0D);
        double passRate = context.records().isEmpty() ? 0D : context.records().stream()
                .filter(record -> record.getPassedFlag() != null && record.getPassedFlag() == 1)
                .count() * 100D / context.records().size();
        double excellentRate = context.records().isEmpty() ? 0D : context.records().stream()
                .filter(record -> value(record.getFinalScore()) >= 90D)
                .count() * 100D / context.records().size();

        List<ExamRecord> sortedRecords = context.records().stream()
                .sorted(Comparator.comparing(ExamRecord::getFinalScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<RankingVO> rankings = sortedRecords.stream()
                .limit(10)
                .map(record -> RankingVO.builder()
                        .rankNo(sortedRecords.indexOf(record) + 1)
                        .candidateName(record.getCandidateName())
                        .examName(record.getExamName())
                        .finalScore(record.getFinalScore())
                        .build())
                .toList();

        List<ScoreBandVO> scoreBands = List.of(
                buildBand("90-100", context.records().stream().filter(item -> value(item.getFinalScore()) >= 90).count()),
                buildBand("80-89", context.records().stream().filter(item -> value(item.getFinalScore()) >= 80 && value(item.getFinalScore()) < 90).count()),
                buildBand("60-79", context.records().stream().filter(item -> value(item.getFinalScore()) >= 60 && value(item.getFinalScore()) < 80).count()),
                buildBand("0-59", context.records().stream().filter(item -> value(item.getFinalScore()) < 60).count())
        );

        List<QuestionScoreRateVO> questionScoreRates = buildQuestionScoreRates(context);
        List<KnowledgePointAnalysisVO> knowledgePoints = buildKnowledgePointAnalysis(context);
        List<OrganizationComparisonVO> organizationComparisons = buildOrganizationComparisons(context);
        List<TrendPointVO> trendPoints = buildTrendPoints(performances, context.plans());

        return AnalysisOverviewVO.builder()
                .totalExamPlans((long) context.plans().size())
                .totalAnswerSheets((long) context.answerSheets().size())
                .averageScore(round(overallAverage))
                .passRate(round(passRate))
                .excellentRate(round(excellentRate))
                .examPerformances(performances)
                .organizationComparisons(organizationComparisons)
                .trendPoints(trendPoints)
                .rankings(rankings)
                .scoreBands(scoreBands)
                .knowledgePoints(knowledgePoints)
                .questionScoreRates(questionScoreRates)
                .build();
    }

    @Override
    public String exportOverviewCsv() {
        AnalysisOverviewVO overview = getOverview();
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("模块,指标,值\n");
        builder.append(line("概览", "考试计划数", String.valueOf(overview.getTotalExamPlans())));
        builder.append(line("概览", "答卷数", String.valueOf(overview.getTotalAnswerSheets())));
        builder.append(line("概览", "平均分", String.valueOf(overview.getAverageScore())));
        builder.append(line("概览", "及格率", String.valueOf(overview.getPassRate())));
        builder.append(line("概览", "优秀率", String.valueOf(overview.getExcellentRate())));
        overview.getExamPerformances().forEach(item -> {
            builder.append(line("考试表现", item.getExamName() + " - 考生数", String.valueOf(item.getCandidateCount())));
            builder.append(line("考试表现", item.getExamName() + " - 已提交", String.valueOf(item.getSubmittedCount())));
            builder.append(line("考试表现", item.getExamName() + " - 已发布", String.valueOf(item.getGradedCount())));
            builder.append(line("考试表现", item.getExamName() + " - 平均分", String.valueOf(item.getAverageScore())));
            builder.append(line("考试表现", item.getExamName() + " - 及格率", String.valueOf(item.getPassRate())));
            builder.append(line("考试表现", item.getExamName() + " - 优秀率", String.valueOf(item.getExcellentRate())));
        });
        overview.getOrganizationComparisons().forEach(item -> {
            builder.append(line("组织对比", item.getOrganizationName() + " - 平均分", String.valueOf(item.getAverageScore())));
            builder.append(line("组织对比", item.getOrganizationName() + " - 及格率", String.valueOf(item.getPassRate())));
            builder.append(line("组织对比", item.getOrganizationName() + " - 优秀率", String.valueOf(item.getExcellentRate())));
        });
        overview.getTrendPoints().forEach(item -> {
            builder.append(line("趋势分析", item.getExamName() + " - 平均分", String.valueOf(item.getAverageScore())));
            builder.append(line("趋势分析", item.getExamName() + " - 及格率", String.valueOf(item.getPassRate())));
            builder.append(line("趋势分析", item.getExamName() + " - 优秀率", String.valueOf(item.getExcellentRate())));
        });
        overview.getScoreBands().forEach(item -> builder.append(line("分数段", item.getBandName(), String.valueOf(item.getCandidateCount()))));
        overview.getKnowledgePoints().forEach(item -> builder.append(line("知识点", item.getKnowledgePoint(), String.valueOf(item.getAverageScoreRate()))));
        overview.getQuestionScoreRates().forEach(item -> builder.append(line("题目得分率", item.getQuestionCode(), String.valueOf(item.getAverageScoreRate()))));
        return builder.toString();
    }

    @Override
    public AnalysisQualityReportVO getQualityReport() {
        AnalyticsContext context = loadContext();
        AnalysisOverviewVO overview = getOverview();
        List<ExamQualityInsightVO> examInsights = buildExamInsights(overview);
        List<KnowledgePointAnalysisVO> weakKnowledgePoints = overview.getKnowledgePoints().stream()
                .sorted(Comparator.comparing(KnowledgePointAnalysisVO::getAverageScoreRate))
                .limit(5)
                .toList();
        List<QuestionScoreRateVO> weakQuestions = overview.getQuestionScoreRates().stream()
                .sorted(Comparator.comparing(QuestionScoreRateVO::getAverageScoreRate))
                .limit(5)
                .toList();

        double completionScore = overview.getExamPerformances().stream()
                .mapToDouble(item -> item.getCandidateCount() == 0 ? 0D : item.getGradedCount() * 100D / item.getCandidateCount())
                .average()
                .orElse(0D);
        double performanceScore = round((overview.getAverageScore() + overview.getPassRate()) / 2D);
        double stabilityScore = round(100D
                - weakQuestions.stream().mapToDouble(item -> Math.max(0D, 60D - value(item.getAverageScoreRate()))).average().orElse(0D)
                - weakKnowledgePoints.stream().mapToDouble(item -> Math.max(0D, 55D - value(item.getAverageScoreRate())) / 2D).average().orElse(0D));
        stabilityScore = Math.max(0D, stabilityScore);
        double overallScore = round((completionScore + performanceScore + stabilityScore) / 3D);

        List<QualityDimensionVO> dimensionScores = List.of(
                QualityDimensionVO.builder()
                        .dimensionName("发布完成度")
                        .score(round(completionScore))
                        .level(level(round(completionScore)))
                        .summary("基于各场考试的已发布人数 / 应参加人数计算，用于反映成绩治理闭环完成度。")
                        .build(),
                QualityDimensionVO.builder()
                        .dimensionName("成绩表现")
                        .score(performanceScore)
                        .level(level(performanceScore))
                        .summary("基于整体平均分和及格率综合计算，用于反映当前考试成绩质量。")
                        .build(),
                QualityDimensionVO.builder()
                        .dimensionName("知识点稳定性")
                        .score(round(stabilityScore))
                        .level(level(round(stabilityScore)))
                        .summary("基于薄弱题目与薄弱知识点的得分率反推稳定性，得分越低表示教学风险越高。")
                        .build()
        );

        return AnalysisQualityReportVO.builder()
                .generatedAt(LocalDateTime.now())
                .overallQualityScore(overallScore)
                .overallQualityLevel(level(overallScore))
                .summary(buildSummary(overallScore, overview, examInsights))
                .riskSummary(buildRiskSummary(weakKnowledgePoints, weakQuestions, context.records()))
                .recommendations(buildRecommendations(overview, weakKnowledgePoints, weakQuestions))
                .dimensionScores(dimensionScores)
                .examInsights(examInsights)
                .weakKnowledgePoints(weakKnowledgePoints)
                .weakQuestions(weakQuestions)
                .build();
    }

    @Override
    public String exportQualityReportMarkdown() {
        AnalysisQualityReportVO report = getQualityReport();
        StringBuilder builder = new StringBuilder();
        builder.append("# 考试质量报告\n\n");
        builder.append("- 生成时间：").append(REPORT_TIME_FORMATTER.format(report.getGeneratedAt())).append('\n');
        builder.append("- 综合质量分：").append(report.getOverallQualityScore()).append('\n');
        builder.append("- 质量等级：").append(report.getOverallQualityLevel()).append("\n\n");

        builder.append("## 一、总体结论\n\n");
        builder.append(report.getSummary()).append("\n\n");

        builder.append("## 二、风险提示\n\n");
        builder.append(report.getRiskSummary()).append("\n\n");

        builder.append("## 三、维度评分\n\n");
        report.getDimensionScores().forEach(item -> builder.append("- ")
                .append(item.getDimensionName())
                .append("：")
                .append(item.getScore())
                .append("（")
                .append(item.getLevel())
                .append("）")
                .append("，")
                .append(item.getSummary())
                .append('\n'));

        builder.append("\n## 四、分考试质量观察\n\n");
        report.getExamInsights().forEach(item -> builder.append("### ")
                .append(item.getExamName())
                .append("\n- 参考人数：").append(item.getCandidateCount())
                .append("\n- 已提交：").append(item.getSubmittedCount())
                .append("\n- 已发布：").append(item.getGradedCount())
                .append("\n- 平均分：").append(item.getAverageScore())
                .append("\n- 及格率：").append(item.getPassRate())
                .append("\n- 质量等级：").append(item.getLevel())
                .append("\n- 结论：").append(item.getSummary())
                .append("\n- 风险：").append(item.getRisk())
                .append("\n\n"));

        builder.append("## 五、薄弱知识点\n\n");
        report.getWeakKnowledgePoints().forEach(item -> builder.append("- ")
                .append(item.getKnowledgePoint())
                .append("：平均得分率 ")
                .append(item.getAverageScoreRate())
                .append("%，作答次数 ")
                .append(item.getAnswerCount())
                .append('\n'));

        builder.append("\n## 六、薄弱题目\n\n");
        report.getWeakQuestions().forEach(item -> builder.append("- ")
                .append(item.getQuestionCode())
                .append("：平均得分率 ")
                .append(item.getAverageScoreRate())
                .append("%，题干 ")
                .append(item.getStem())
                .append('\n'));

        builder.append("\n## 七、建议动作\n\n");
        report.getRecommendations().forEach(item -> builder.append("- ").append(item).append('\n'));
        return builder.toString();
    }

    private AnalyticsContext loadContext() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<ExamPlan> plans = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                .in(!accessScopeService.isAdmin(), ExamPlan::getOrganizationId, accessibleOrgIds)
                .orderByDesc(ExamPlan::getStartTime, ExamPlan::getId));
        List<Long> planIds = plans.stream().map(ExamPlan::getId).toList();
        List<ExamRecord> records = examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                .in(!accessScopeService.isAdmin(), ExamRecord::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds));
        List<AnswerSheet> answerSheets = answerSheetMapper.selectList(Wrappers.lambdaQuery(AnswerSheet.class)
                .in(!accessScopeService.isAdmin(), AnswerSheet::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds));
        List<Long> answerSheetIds = answerSheets.stream().map(AnswerSheet::getId).toList();
        List<AnswerItem> answerItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                .in(!accessScopeService.isAdmin(), AnswerItem::getAnswerSheetId, answerSheetIds.isEmpty() ? List.of(-1L) : answerSheetIds));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(answerItems.stream()
                        .map(AnswerItem::getQuestionId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        return new AnalyticsContext(plans, records, answerSheets, answerItems, questionMap);
    }

    private List<QuestionScoreRateVO> buildQuestionScoreRates(AnalyticsContext context) {
        return context.answerItems().stream()
                .collect(Collectors.groupingBy(AnswerItem::getQuestionId))
                .entrySet()
                .stream()
                .map(entry -> {
                    QuestionBank question = context.questionMap().get(entry.getKey());
                    double avgRate = entry.getValue().stream()
                            .mapToDouble(item -> item.getMaxScore() == null || item.getMaxScore() == 0 ? 0D : value(item.getScoreAwarded()) / item.getMaxScore() * 100D)
                            .average()
                            .orElse(0D);
                    return QuestionScoreRateVO.builder()
                            .questionId(entry.getKey())
                            .questionCode(question == null ? null : question.getQuestionCode())
                            .stem(question == null ? null : question.getStem())
                            .averageScoreRate(round(avgRate))
                            .answerCount((long) entry.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparing(QuestionScoreRateVO::getAverageScoreRate))
                .toList();
    }

    private List<KnowledgePointAnalysisVO> buildKnowledgePointAnalysis(AnalyticsContext context) {
        return context.answerItems().stream()
                .collect(Collectors.groupingBy(item -> {
                    QuestionBank question = context.questionMap().get(item.getQuestionId());
                    return question == null || question.getKnowledgePoint() == null || question.getKnowledgePoint().isBlank()
                            ? "未分类知识点"
                            : question.getKnowledgePoint();
                }))
                .entrySet()
                .stream()
                .map(entry -> KnowledgePointAnalysisVO.builder()
                        .knowledgePoint(entry.getKey())
                        .averageScoreRate(round(entry.getValue().stream()
                                .mapToDouble(item -> item.getMaxScore() == null || item.getMaxScore() == 0 ? 0D : value(item.getScoreAwarded()) / item.getMaxScore() * 100D)
                                .average()
                                .orElse(0D)))
                        .answerCount((long) entry.getValue().size())
                        .build())
                .sorted(Comparator.comparing(KnowledgePointAnalysisVO::getAverageScoreRate))
                .toList();
    }

    private List<ExamQualityInsightVO> buildExamInsights(AnalysisOverviewVO overview) {
        return overview.getExamPerformances().stream()
                .map(item -> {
                    double completion = item.getCandidateCount() == 0 ? 0D : item.getGradedCount() * 100D / item.getCandidateCount();
                    double score = round((completion + item.getAverageScore() + item.getPassRate()) / 3D);
                    return ExamQualityInsightVO.builder()
                            .examPlanId(item.getExamPlanId())
                            .examName(item.getExamName())
                            .candidateCount(item.getCandidateCount())
                            .submittedCount(item.getSubmittedCount())
                            .gradedCount(item.getGradedCount())
                            .averageScore(item.getAverageScore())
                            .passRate(item.getPassRate())
                            .level(level(score))
                            .summary(buildExamSummary(item, completion))
                            .risk(buildExamRisk(item, completion))
                            .build();
                })
                .toList();
    }

    private List<OrganizationComparisonVO> buildOrganizationComparisons(AnalyticsContext context) {
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(context.records().stream()
                        .map(ExamRecord::getUserId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> right));
        return context.records().stream()
                .collect(Collectors.groupingBy(record -> resolveOrganizationName(record, userMap)))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ExamRecord> items = entry.getValue();
                    double average = items.stream().map(ExamRecord::getFinalScore).mapToDouble(this::value).average().orElse(0D);
                    double passRate = items.isEmpty() ? 0D : items.stream()
                            .filter(item -> item.getPassedFlag() != null && item.getPassedFlag() == 1)
                            .count() * 100D / items.size();
                    double excellentRate = items.isEmpty() ? 0D : items.stream()
                            .filter(item -> value(item.getFinalScore()) >= 90D)
                            .count() * 100D / items.size();
                    return OrganizationComparisonVO.builder()
                            .organizationName(entry.getKey())
                            .candidateCount((long) items.size())
                            .averageScore(round(average))
                            .passRate(round(passRate))
                            .excellentRate(round(excellentRate))
                            .build();
                })
                .sorted(Comparator.comparing(OrganizationComparisonVO::getAverageScore).reversed())
                .limit(8)
                .toList();
    }

    private List<TrendPointVO> buildTrendPoints(List<ExamPerformanceVO> performances, List<ExamPlan> plans) {
        Map<Long, ExamPlan> planMap = plans.stream().collect(Collectors.toMap(ExamPlan::getId, Function.identity(), (left, right) -> right));
        return performances.stream()
                .sorted(Comparator.comparing(item -> {
                    ExamPlan plan = planMap.get(item.getExamPlanId());
                    return plan == null ? LocalDateTime.MIN : plan.getStartTime();
                }))
                .map(item -> {
                    ExamPlan plan = planMap.get(item.getExamPlanId());
                    return TrendPointVO.builder()
                            .examPlanId(item.getExamPlanId())
                            .examName(item.getExamName())
                            .periodLabel(plan == null || plan.getStartTime() == null ? item.getExamName() : REPORT_TIME_FORMATTER.format(plan.getStartTime()))
                            .averageScore(item.getAverageScore())
                            .passRate(item.getPassRate())
                            .excellentRate(item.getExcellentRate())
                            .build();
                })
                .limit(8)
                .toList();
    }

    private String buildSummary(double overallScore, AnalysisOverviewVO overview, List<ExamQualityInsightVO> examInsights) {
        long riskExamCount = examInsights.stream()
                .filter(item -> "中风险".equals(item.getLevel()) || "高风险".equals(item.getLevel()))
                .count();
        return "当前统计范围内共有 " + overview.getTotalExamPlans()
                + " 场考试、" + overview.getTotalAnswerSheets()
                + " 份答卷，综合质量分为 " + overallScore
                + "，质量等级为" + level(overallScore)
                + "。其中存在 " + riskExamCount
                + " 场考试需要重点关注发布完成度、及格率或薄弱知识点表现。";
    }

    private String buildRiskSummary(List<KnowledgePointAnalysisVO> weakKnowledgePoints,
                                    List<QuestionScoreRateVO> weakQuestions,
                                    List<ExamRecord> records) {
        long unresolved = records.stream()
                .filter(item -> item.getReviewStatus() != null && !"APPROVED".equals(item.getReviewStatus()))
                .count();
        String weakKnowledge = weakKnowledgePoints.isEmpty() ? "暂无明显薄弱知识点" : weakKnowledgePoints.get(0).getKnowledgePoint();
        String weakQuestion = weakQuestions.isEmpty() ? "暂无明显薄弱题目" : weakQuestions.get(0).getQuestionCode();
        return "当前待继续关注的风险主要包括：未完全闭环的成绩治理记录 " + unresolved
                + " 条、薄弱知识点重点集中在“" + weakKnowledge
                + "”、薄弱题目重点集中在“" + weakQuestion + "”。";
    }

    private List<String> buildRecommendations(AnalysisOverviewVO overview,
                                              List<KnowledgePointAnalysisVO> weakKnowledgePoints,
                                              List<QuestionScoreRateVO> weakQuestions) {
        String weakestKnowledgePoint = weakKnowledgePoints.isEmpty() ? "未分类知识点" : weakKnowledgePoints.get(0).getKnowledgePoint();
        String weakestQuestion = weakQuestions.isEmpty() ? "暂无" : weakQuestions.get(0).getQuestionCode();
        return List.of(
                "优先复盘平均分或及格率偏低的考试，结合班级授课计划调整讲评安排。",
                "针对“" + weakestKnowledgePoint + "”补讲知识点和专项练习，避免同类失分持续扩大。",
                "对题目“" + weakestQuestion + "”检查题干表述、评分标准与知识点覆盖是否合理。",
                "持续跟踪已发布成绩的复核和申诉状态，确保成绩治理闭环可追溯。"
        );
    }

    private String buildExamSummary(ExamPerformanceVO item, double completion) {
        if (item.getPassRate() >= 80 && completion >= 90) {
            return "整体表现稳定，成绩发布完成度较高，可作为当前教学效果的正向样本。";
        }
        if (item.getPassRate() < 60) {
            return "及格率偏低，建议结合薄弱知识点和失分题目进行专项讲评。";
        }
        if (completion < 80) {
            return "成绩发布完成度偏低，建议优先收口阅卷和复核。";
        }
        return "整体处于可接受区间，但仍建议继续关注成绩分布和知识点掌握差异。";
    }

    private String buildExamRisk(ExamPerformanceVO item, double completion) {
        if (completion < 80) {
            return "已发布人数占比偏低，存在成绩治理收口风险。";
        }
        if (item.getPassRate() < 60) {
            return "及格率偏低，存在教学质量风险。";
        }
        if (item.getLowestScore() < 20) {
            return "最低分过低，建议核查试题难度和评分口径。";
        }
        return "当前无明显高风险，但仍需持续关注。";
    }

    private String resolveOrganizationName(ExamRecord record, Map<Long, SysUser> userMap) {
        SysUser user = userMap.get(record.getUserId());
        if (user == null || user.getOrganizationName() == null || user.getOrganizationName().isBlank()) {
            return "未知组织";
        }
        return user.getOrganizationName();
    }

    private String level(double score) {
        if (score >= 85) {
            return "低风险";
        }
        if (score >= 65) {
            return "中风险";
        }
        return "高风险";
    }

    private int countAnswerSheets(Long examPlanId, List<AnswerSheet> sheets) {
        return (int) sheets.stream().filter(item -> examPlanId.equals(item.getExamPlanId())).count();
    }

    private ScoreBandVO buildBand(String bandName, long count) {
        return ScoreBandVO.builder().bandName(bandName).candidateCount(count).build();
    }

    private double value(Double value) {
        return value == null ? 0D : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String line(String module, String metric, String value) {
        return csv(module) + "," + csv(metric) + "," + csv(value) + "\n";
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record AnalyticsContext(
            List<ExamPlan> plans,
            List<ExamRecord> records,
            List<AnswerSheet> answerSheets,
            List<AnswerItem> answerItems,
            Map<Long, QuestionBank> questionMap
    ) {
    }
}
