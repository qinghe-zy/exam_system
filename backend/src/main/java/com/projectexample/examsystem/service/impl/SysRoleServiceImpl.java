package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.SysRole;
import com.projectexample.examsystem.mapper.SysRoleMapper;
import com.projectexample.examsystem.service.SysRoleService;
import com.projectexample.examsystem.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public List<SysRoleVO> listRoles() {
        return sysRoleMapper.selectList(Wrappers.lambdaQuery(SysRole.class)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(role -> SysRoleVO.builder()
                        .id(role.getId())
                        .roleCode(role.getRoleCode())
                        .roleName(role.getRoleName())
                        .remark(role.getRemark())
                        .build())
                .toList();
    }
}
