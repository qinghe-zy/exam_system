package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.CandidateImportItemRequest;
import com.projectexample.examsystem.dto.CandidateImportRequest;
import com.projectexample.examsystem.dto.SysUserSaveRequest;
import com.projectexample.examsystem.entity.Organization;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.OrganizationMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.service.SysUserService;
import com.projectexample.examsystem.vo.CandidateImportResultVO;
import com.projectexample.examsystem.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final OrganizationMapper organizationMapper;
    private final PasswordEncoder passwordEncoder;

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
                .map(this::toVO)
                .toList();
    }

    @Override
    public SysUserVO createUser(SysUserSaveRequest request) {
        if (findByUsername(request.getUsername()) != null) {
            throw new BusinessException(4009, "Username already exists");
        }
        SysUser entity = new SysUser();
        apply(entity, request, true);
        sysUserMapper.insert(entity);
        return toVO(requireEntity(entity.getId()));
    }

    @Override
    public SysUserVO updateUser(Long id, SysUserSaveRequest request) {
        SysUser entity = requireEntity(id);
        SysUser duplicate = findByUsername(request.getUsername());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new BusinessException(4009, "Username already exists");
        }
        apply(entity, request, false);
        sysUserMapper.updateById(entity);
        return toVO(requireEntity(id));
    }

    @Override
    public CandidateImportResultVO importCandidates(CandidateImportRequest request) {
        List<String> imported = new ArrayList<>();
        for (CandidateImportItemRequest item : request.getItems()) {
            SysUser existing = findByUsername(item.getUsername());
            if (existing != null) {
                throw new BusinessException(4011, "Duplicate username in import: " + item.getUsername());
            }
            Organization organization = requireOrganization(item.getOrganizationId());
            SysUser entity = new SysUser();
            entity.setUsername(item.getUsername());
            entity.setNickname(item.getFullName());
            entity.setFullName(item.getFullName());
            entity.setRoleCode("STUDENT");
            entity.setOrganizationId(organization.getId());
            entity.setOrganizationName(organization.getOrgName());
            entity.setDepartmentName(item.getDepartmentName());
            entity.setEmail(item.getEmail());
            entity.setPhone(item.getPhone());
            entity.setCandidateNo(item.getCandidateNo());
            entity.setPassword(passwordEncoder.encode("student123"));
            entity.setStatus(1);
            sysUserMapper.insert(entity);
            imported.add(item.getUsername());
        }
        return CandidateImportResultVO.builder()
                .importedCount(imported.size())
                .usernames(imported)
                .build();
    }

    private SysUser requireEntity(Long id) {
        SysUser entity = sysUserMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(4040, "User not found");
        }
        return entity;
    }

    private Organization requireOrganization(Long organizationId) {
        Organization organization = organizationMapper.selectById(organizationId);
        if (organization == null) {
            throw new BusinessException(4040, "Organization not found");
        }
        return organization;
    }

    private void apply(SysUser entity, SysUserSaveRequest request, boolean createMode) {
        Organization organization = requireOrganization(request.getOrganizationId());
        entity.setUsername(request.getUsername());
        entity.setNickname(request.getNickname());
        entity.setFullName(request.getFullName());
        entity.setRoleCode(request.getRoleCode());
        entity.setOrganizationId(organization.getId());
        entity.setOrganizationName(organization.getOrgName());
        entity.setDepartmentName(request.getDepartmentName());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setCandidateNo(request.getCandidateNo());
        entity.setStatus(request.getStatus());
        if (createMode || StringUtils.hasText(request.getPassword())) {
            entity.setPassword(passwordEncoder.encode(StringUtils.hasText(request.getPassword()) ? request.getPassword() : "ChangeMe123!"));
        }
    }

    private SysUserVO toVO(SysUser user) {
        return SysUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .fullName(user.getFullName())
                .roleCode(user.getRoleCode())
                .organizationId(user.getOrganizationId())
                .organizationName(user.getOrganizationName())
                .departmentName(user.getDepartmentName())
                .email(user.getEmail())
                .status(user.getStatus())
                .build();
    }
}
