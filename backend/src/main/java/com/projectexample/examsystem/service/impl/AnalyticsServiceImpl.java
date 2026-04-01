package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.service.AnalyticsService;
import com.projectexample.examsystem.vo.AnalysisOverviewVO;
import com.projectexample.examsystem.vo.ExamPerformanceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final ExamRecordMapper examRecordMapper;

    @Override
    public AnalysisOverviewVO getOverview() {
        List<ExamPlan> plans = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).orderByDesc(ExamPlan::getStartTime, ExamPlan::getId));
        List<ExamRecord> records = examRecordMapper.selectList(null);
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

        return AnalysisOverviewVO.builder()
                .totalExamPlans((long) plans.size())
                .totalAnswerSheets(answerSheetMapper.selectCount(null))
                .averageScore(round(overallAverage))
                .passRate(round(passRate))
                .examPerformances(performances)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
