package com.projectexample.examsystem.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.Organization;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.OrganizationMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccessScopeService {

    private final SysUserMapper sysUserMapper;
    private final OrganizationMapper organizationMapper;

    public AccessScopeService(SysUserMapper sysUserMapper, OrganizationMapper organizationMapper) {
        this.sysUserMapper = sysUserMapper;
        this.organizationMapper = organizationMapper;
    }

    public SysUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new BusinessException(4010, "Current user is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        String username = principal instanceof UserPrincipal userPrincipal ? userPrincipal.getUsername() : String.valueOf(principal);
        SysUser user = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, username).last("limit 1"));
        if (user == null) {
            throw new BusinessException(4010, "Current user is invalid");
        }
        return user;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(currentUser().getRoleCode());
    }

    public Long currentOrganizationId() {
        return currentUser().getOrganizationId();
    }

    public List<Long> accessibleOrganizationIds() {
        SysUser user = currentUser();
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return organizationMapper.selectList(null).stream().map(Organization::getId).toList();
        }
        if (user.getOrganizationId() == null) {
            return List.of();
        }
        List<Organization> all = organizationMapper.selectList(null);
        List<Long> ids = new ArrayList<>();
        collectSubtree(user.getOrganizationId(), all, ids);
        return ids;
    }

    public List<Long> visibleOrganizationIds() {
        SysUser user = currentUser();
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return organizationMapper.selectList(null).stream().map(Organization::getId).toList();
        }
        if (user.getOrganizationId() == null) {
            return List.of();
        }
        List<Organization> all = organizationMapper.selectList(null);
        List<Long> ids = new ArrayList<>();
        collectAncestors(user.getOrganizationId(), all, ids);
        collectSubtree(user.getOrganizationId(), all, ids);
        return ids.stream().distinct().toList();
    }

    public void assertOrganizationAccessible(Long organizationId) {
        if (organizationId == null) {
            return;
        }
        if (isAdmin()) {
            return;
        }
        if (!accessibleOrganizationIds().contains(organizationId)) {
            throw new BusinessException(4031, "Current user cannot access the target organization");
        }
    }

    private void collectSubtree(Long rootId, List<Organization> all, List<Long> ids) {
        ids.add(rootId);
        all.stream()
                .filter(item -> rootId.equals(item.getParentId()))
                .forEach(item -> collectSubtree(item.getId(), all, ids));
    }

    private void collectAncestors(Long currentId, List<Organization> all, List<Long> ids) {
        Organization current = all.stream()
                .filter(item -> currentId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (current == null) {
            return;
        }
        ids.add(current.getId());
        Long parentId = current.getParentId();
        if (parentId == null || parentId == 0L || parentId.equals(current.getId())) {
            return;
        }
        collectAncestors(parentId, all, ids);
    }
}
