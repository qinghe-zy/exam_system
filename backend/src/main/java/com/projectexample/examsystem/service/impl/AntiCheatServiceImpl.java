package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AntiCheatEvent;
import com.projectexample.examsystem.mapper.AntiCheatEventMapper;
import com.projectexample.examsystem.service.AntiCheatService;
import com.projectexample.examsystem.vo.AntiCheatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AntiCheatServiceImpl implements AntiCheatService {

    private final AntiCheatEventMapper antiCheatEventMapper;

    @Override
    public List<AntiCheatEventVO> listEvents() {
        return antiCheatEventMapper.selectList(Wrappers.lambdaQuery(AntiCheatEvent.class)
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
