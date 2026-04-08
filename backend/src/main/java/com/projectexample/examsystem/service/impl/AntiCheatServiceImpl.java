package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.AntiCheatEvent;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.AntiCheatEventMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.AntiCheatService;
import com.projectexample.examsystem.vo.AntiCheatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AntiCheatServiceImpl implements AntiCheatService {

    private final AntiCheatEventMapper antiCheatEventMapper;
    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final SysUserMapper sysUserMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<AntiCheatEventVO> listEvents() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> planIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).in(ExamPlan::getOrganizationId, accessibleOrgIds))
                .stream().map(ExamPlan::getId).toList();
        List<AntiCheatEvent> events = antiCheatEventMapper.selectList(Wrappers.lambdaQuery(AntiCheatEvent.class)
                        .in(!accessScopeService.isAdmin(), AntiCheatEvent::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .orderByDesc(AntiCheatEvent::getOccurredAt, AntiCheatEvent::getId));
        Map<Long, ExamPlan> planMap = examPlanMapper.selectBatchIds(events.stream().map(AntiCheatEvent::getExamPlanId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ExamPlan::getId, Function.identity()));
        Map<Long, AnswerSheet> sheetMap = answerSheetMapper.selectBatchIds(events.stream()
                        .map(AntiCheatEvent::getAnswerSheetId)
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(AnswerSheet::getId, Function.identity()));
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(events.stream().map(AntiCheatEvent::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return events.stream()
                .map(event -> AntiCheatEventVO.builder()
                        .id(event.getId())
                        .examPlanId(event.getExamPlanId())
                        .examName(planMap.get(event.getExamPlanId()) == null ? null : planMap.get(event.getExamPlanId()).getExamName())
                        .answerSheetId(event.getAnswerSheetId())
                        .userId(event.getUserId())
                        .candidateName(resolveCandidateName(event, sheetMap, userMap))
                        .eventType(event.getEventType())
                        .severity(event.getSeverity())
                        .leaveCount(event.getLeaveCount())
                        .triggeredAutoSave(event.getTriggeredAutoSave())
                        .saveVersion(event.getSaveVersion())
                        .clientIp(event.getClientIp())
                        .deviceFingerprint(event.getDeviceFingerprint())
                        .deviceInfo(event.getDeviceInfo())
                        .detailText(event.getDetailText())
                        .occurredAt(event.getOccurredAt())
                        .build())
                .toList();
    }

    private String resolveCandidateName(AntiCheatEvent event,
                                        Map<Long, AnswerSheet> sheetMap,
                                        Map<Long, SysUser> userMap) {
        if (event.getAnswerSheetId() != null) {
            AnswerSheet sheet = sheetMap.get(event.getAnswerSheetId());
            if (sheet != null) {
                return sheet.getCandidateName();
            }
        }
        SysUser user = userMap.get(event.getUserId());
        if (user == null) {
            return null;
        }
        return user.getFullName() == null ? user.getNickname() : user.getFullName();
    }
}
