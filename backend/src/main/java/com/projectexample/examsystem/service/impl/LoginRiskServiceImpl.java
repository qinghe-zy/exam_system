package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.LoginRiskLog;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.LoginRiskLogMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.LoginRiskService;
import com.projectexample.examsystem.vo.LoginRiskLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginRiskServiceImpl implements LoginRiskService {

    private final LoginRiskLogMapper loginRiskLogMapper;
    private final SysUserMapper sysUserMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<LoginRiskLogVO> listLogs() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<LoginRiskLog> logs = loginRiskLogMapper.selectList(Wrappers.lambdaQuery(LoginRiskLog.class)
                .orderByDesc(LoginRiskLog::getLoginAt, LoginRiskLog::getId));
        if (accessScopeService.isAdmin()) {
            return logs.stream().map(this::toVO).toList();
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
                        .in(SysUser::getOrganizationId, accessibleOrgIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return logs.stream()
                .filter(log -> log.getUserId() != null && userMap.containsKey(log.getUserId()))
                .map(this::toVO)
                .toList();
    }

    private LoginRiskLogVO toVO(LoginRiskLog entity) {
        return LoginRiskLogVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .userId(entity.getUserId())
                .roleCode(entity.getRoleCode())
                .successFlag(entity.getSuccessFlag())
                .clientIp(entity.getClientIp())
                .userAgent(entity.getUserAgent())
                .deviceFingerprint(entity.getDeviceFingerprint())
                .deviceInfo(entity.getDeviceInfo())
                .riskLevel(entity.getRiskLevel())
                .riskReason(entity.getRiskReason())
                .loginAt(entity.getLoginAt())
                .build();
    }
}
