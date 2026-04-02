package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.ExamRecordService;
import com.projectexample.examsystem.vo.ExamRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamRecordServiceImpl implements ExamRecordService {

    private final ExamRecordMapper examRecordMapper;
    private final ExamPlanMapper examPlanMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<ExamRecordVO> listRecords() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> planIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).in(ExamPlan::getOrganizationId, accessibleOrgIds))
                .stream().map(ExamPlan::getId).toList();

        return examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                        .in(!accessScopeService.isAdmin(), ExamRecord::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .orderByDesc(ExamRecord::getSubmittedAt, ExamRecord::getId))
                .stream()
                .map(record -> ExamRecordVO.builder()
                        .id(record.getId())
                        .candidateName(record.getCandidateName())
                        .examName(record.getExamName())
                        .paperName(record.getPaperName())
                        .submittedAt(record.getSubmittedAt())
                        .objectiveScore(record.getObjectiveScore())
                        .subjectiveScore(record.getSubjectiveScore())
                        .finalScore(record.getFinalScore())
                        .passedFlag(record.getPassedFlag())
                        .publishedFlag(record.getPublishedFlag())
                        .status(record.getStatus())
                        .build())
                .toList();
    }
}
