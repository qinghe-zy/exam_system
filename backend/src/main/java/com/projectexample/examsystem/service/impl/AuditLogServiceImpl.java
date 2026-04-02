package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.mapper.AuditLogMapper;
import com.projectexample.examsystem.service.AuditLogService;
import com.projectexample.examsystem.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public List<AuditLogVO> listLogs() {
        return auditLogMapper.selectList(Wrappers.lambdaQuery(AuditLog.class)
                        .orderByDesc(AuditLog::getCreateTime, AuditLog::getId))
                .stream()
                .map(item -> AuditLogVO.builder()
                        .id(item.getId())
                        .operatorName(item.getOperatorName())
                        .moduleName(item.getModuleName())
                        .actionName(item.getActionName())
                        .targetType(item.getTargetType())
                        .targetId(item.getTargetId())
                        .detailText(item.getDetailText())
                        .createTime(item.getCreateTime())
                        .build())
                .toList();
    }
}
