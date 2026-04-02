package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.AnalyticsService;
import com.projectexample.examsystem.vo.AnalysisOverviewVO;
import com.projectexample.examsystem.vo.ExamPerformanceVO;
import com.projectexample.examsystem.vo.KnowledgePointAnalysisVO;
import com.projectexample.examsystem.vo.QuestionScoreRateVO;
import com.projectexample.examsystem.vo.RankingVO;
import com.projectexample.examsystem.vo.ScoreBandVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerItemMapper answerItemMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public AnalysisOverviewVO getOverview() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<ExamPlan> plans = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                .in(!accessScopeService.isAdmin(), ExamPlan::getOrganizationId, accessibleOrgIds)
                .orderByDesc(ExamPlan::getStartTime, ExamPlan::getId));
        List<Long> planIds = plans.stream().map(ExamPlan::getId).toList();
        List<ExamRecord> records = examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                .in(!accessScopeService.isAdmin(), ExamRecord::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds));
        List<Long> answerSheetIds = answerSheetMapper.selectList(Wrappers.lambdaQuery(AnswerSheet.class)
                .in(!accessScopeService.isAdmin(), AnswerSheet::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds))
                .stream().map(AnswerSheet::getId).toList();
        List<AnswerItem> answerItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                .in(!accessScopeService.isAdmin(), AnswerItem::getAnswerSheetId, answerSheetIds.isEmpty() ? List.of(-1L) : answerSheetIds));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(answerItems.stream().map(AnswerItem::getQuestionId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        List<ExamPerformanceVO> performances = plans.stream().map(plan -> {
            List<ExamRecord> currentRecords = records.stream()
                    .filter(record -> plan.getId().equals(record.getExamPlanId()))
                    .toList();
            int candidateCount = answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class).eq(AnswerSheet::getExamPlanId, plan.getId())).intValue();
            int submittedCount = currentRecords.size();
            int gradedCount = (int) currentRecords.stream().filter(record -> "PUBLISHED".equals(record.getStatus())).count();
            double average = currentRecords.stream().map(ExamRecord::getFinalScore).mapToDouble(value -> value == null ? 0D : value).average().orElse(0D);
            double highest = currentRecords.stream().map(ExamRecord::getFinalScore).mapToDouble(value -> value == null ? 0D : value).max().orElse(0D);
            double lowest = currentRecords.stream().map(ExamRecord::getFinalScore).mapToDouble(value -> value == null ? 0D : value).min().orElse(0D);
            double passRate = currentRecords.isEmpty() ? 0D : currentRecords.stream().filter(record -> record.getPassedFlag() != null && record.getPassedFlag() == 1).count() * 100D / currentRecords.size();
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
                    .build();
        }).toList();

        double overallAverage = records.stream().map(ExamRecord::getFinalScore).mapToDouble(value -> value == null ? 0D : value).average().orElse(0D);
        double passRate = records.isEmpty() ? 0D : records.stream().filter(record -> record.getPassedFlag() != null && record.getPassedFlag() == 1).count() * 100D / records.size();

        List<RankingVO> rankings = records.stream()
                .sorted(Comparator.comparing(ExamRecord::getFinalScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    for (int index = 0; index < list.size(); index++) {
                        list.set(index, list.get(index));
                    }
                    return list;
                }))
                .stream()
                .map(record -> RankingVO.builder()
                        .rankNo(records.stream().sorted(Comparator.comparing(ExamRecord::getFinalScore, Comparator.nullsLast(Comparator.reverseOrder()))).toList().indexOf(record) + 1)
                        .candidateName(record.getCandidateName())
                        .examName(record.getExamName())
                        .finalScore(record.getFinalScore())
                        .build())
                .toList();

        List<ScoreBandVO> scoreBands = List.of(
                buildBand("90-100", records.stream().filter(item -> value(item.getFinalScore()) >= 90).count()),
                buildBand("80-89", records.stream().filter(item -> value(item.getFinalScore()) >= 80 && value(item.getFinalScore()) < 90).count()),
                buildBand("60-79", records.stream().filter(item -> value(item.getFinalScore()) >= 60 && value(item.getFinalScore()) < 80).count()),
                buildBand("0-59", records.stream().filter(item -> value(item.getFinalScore()) < 60).count())
        );

        List<QuestionScoreRateVO> questionScoreRates = answerItems.stream()
                .collect(Collectors.groupingBy(AnswerItem::getQuestionId))
                .entrySet()
                .stream()
                .map(entry -> {
                    QuestionBank question = questionMap.get(entry.getKey());
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

        List<KnowledgePointAnalysisVO> knowledgePoints = answerItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    QuestionBank question = questionMap.get(item.getQuestionId());
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

        return AnalysisOverviewVO.builder()
                .totalExamPlans((long) plans.size())
                .totalAnswerSheets(answerSheetMapper.selectCount(null))
                .averageScore(round(overallAverage))
                .passRate(round(passRate))
                .examPerformances(performances)
                .rankings(rankings)
                .scoreBands(scoreBands)
                .knowledgePoints(knowledgePoints)
                .questionScoreRates(questionScoreRates)
                .build();
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
}
