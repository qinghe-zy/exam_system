package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.service.SysUserService;
import com.projectexample.examsystem.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;

    @Override
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
    }

    @Override
    public List<SysUserVO> listUsers() {
        return sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(user -> SysUserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .fullName(user.getFullName())
                        .roleCode(user.getRoleCode())
                        .organizationName(user.getOrganizationName())
                        .departmentName(user.getDepartmentName())
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .build())
                .toList();
    }
}
