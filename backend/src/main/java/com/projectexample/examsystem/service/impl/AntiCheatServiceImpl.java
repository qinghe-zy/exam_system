package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AntiCheatEvent;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.mapper.AntiCheatEventMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.AntiCheatService;
import com.projectexample.examsystem.vo.AntiCheatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AntiCheatServiceImpl implements AntiCheatService {

    private final AntiCheatEventMapper antiCheatEventMapper;
    private final ExamPlanMapper examPlanMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<AntiCheatEventVO> listEvents() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> planIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).in(ExamPlan::getOrganizationId, accessibleOrgIds))
                .stream().map(ExamPlan::getId).toList();
        return antiCheatEventMapper.selectList(Wrappers.lambdaQuery(AntiCheatEvent.class)
                        .in(!accessScopeService.isAdmin(), AntiCheatEvent::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .orderByDesc(AntiCheatEvent::getOccurredAt, AntiCheatEvent::getId))
                .stream()
                .map(event -> AntiCheatEventVO.builder()
                        .id(event.getId())
                        .examPlanId(event.getExamPlanId())
                        .answerSheetId(event.getAnswerSheetId())
                        .userId(event.getUserId())
                        .eventType(event.getEventType())
                        .severity(event.getSeverity())
                        .detailText(event.getDetailText())
                        .occurredAt(event.getOccurredAt())
                        .build())
                .toList();
    }
}
