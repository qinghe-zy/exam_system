package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.AuditLogMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.AuditLogService;
import com.projectexample.examsystem.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final SysUserMapper sysUserMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<AuditLogVO> listLogs() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> operatorIds = accessScopeService.isAdmin()
                ? sysUserMapper.selectList(null).stream().map(SysUser::getId).toList()
                : sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class).in(SysUser::getOrganizationId, accessibleOrgIds))
                .stream().map(SysUser::getId).toList();
        return auditLogMapper.selectList(Wrappers.lambdaQuery(AuditLog.class)
                        .in(!accessScopeService.isAdmin(), AuditLog::getOperatorId, operatorIds.isEmpty() ? List.of(-1L) : operatorIds)
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
