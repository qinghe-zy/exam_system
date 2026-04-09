package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.OrganizationSaveRequest;
import com.projectexample.examsystem.entity.Organization;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.OrganizationMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.security.ExamPeriodProtectionService;
import com.projectexample.examsystem.service.OrganizationService;
import com.projectexample.examsystem.vo.OrganizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final AccessScopeService accessScopeService;
    private final ExamPeriodProtectionService examPeriodProtectionService;

    @Override
    public List<OrganizationVO> listOrganizations() {
        List<Long> accessibleIds = accessScopeService.accessibleOrganizationIds();
        List<Organization> scoped = organizationMapper.selectList(Wrappers.lambdaQuery(Organization.class)
                .in(!accessScopeService.isAdmin(), Organization::getId, accessibleIds)
                .orderByAsc(Organization::getParentId, Organization::getId));
        return accessScopeService.isAdmin()
                ? buildTree(scoped)
                : buildScopedTree(scoped, accessScopeService.currentOrganizationId());
    }

    @Override
    public OrganizationVO createOrganization(OrganizationSaveRequest request) {
        examPeriodProtectionService.assertMutable("新增组织");
        accessScopeService.assertOrganizationAccessible(request.getParentId());
        Organization entity = new Organization();
        apply(entity, request);
        organizationMapper.insert(entity);
        return toVO(requireEntity(entity.getId()));
    }

    @Override
    public OrganizationVO updateOrganization(Long id, OrganizationSaveRequest request) {
        examPeriodProtectionService.assertMutable("更新组织");
        Organization entity = requireEntity(id);
        accessScopeService.assertOrganizationAccessible(entity.getId());
        accessScopeService.assertOrganizationAccessible(request.getParentId());
        apply(entity, request);
        organizationMapper.updateById(entity);
        return toVO(requireEntity(id));
    }

    @Override
    public void deleteOrganization(Long id) {
        examPeriodProtectionService.assertMutable("删除组织");
        requireEntity(id);
        accessScopeService.assertOrganizationAccessible(id);
        long children = organizationMapper.selectCount(Wrappers.lambdaQuery(Organization.class).eq(Organization::getParentId, id));
        if (children > 0) {
            throw new BusinessException(4008, "Please delete child organizations first");
        }
        organizationMapper.deleteById(id);
    }

    private Organization requireEntity(Long id) {
        Organization entity = organizationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(4040, "Organization not found");
        }
        return entity;
    }

    private void apply(Organization entity, OrganizationSaveRequest request) {
        entity.setOrgCode(request.getOrgCode());
        entity.setOrgName(request.getOrgName());
        entity.setOrgType(request.getOrgType());
        entity.setParentId(request.getParentId());
        entity.setStatus(request.getStatus());
    }

    private List<OrganizationVO> buildTree(List<Organization> organizations) {
        Map<Long, List<OrganizationVO>> grouped = organizations.stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(OrganizationVO::getId))
                .collect(Collectors.groupingBy(item -> item.getParentId() == null ? 0L : item.getParentId()));
        List<OrganizationVO> roots = grouped.getOrDefault(0L, List.of());
        roots.forEach(root -> attach(root, grouped));
        return roots;
    }

    private List<OrganizationVO> buildScopedTree(List<Organization> organizations, Long rootId) {
        Map<Long, List<OrganizationVO>> grouped = organizations.stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(OrganizationVO::getId))
                .collect(Collectors.groupingBy(item -> item.getParentId() == null ? 0L : item.getParentId()));
        List<OrganizationVO> roots = organizations.stream()
                .filter(item -> item.getId().equals(rootId))
                .map(this::toVO)
                .toList();
        roots.forEach(root -> attach(root, grouped));
        return roots;
    }

    private void attach(OrganizationVO node, Map<Long, List<OrganizationVO>> grouped) {
        List<OrganizationVO> children = grouped.getOrDefault(node.getId(), List.of());
        node.setChildren(children);
        children.forEach(child -> attach(child, grouped));
    }

    private OrganizationVO toVO(Organization entity) {
        return OrganizationVO.builder()
                .id(entity.getId())
                .orgCode(entity.getOrgCode())
                .orgName(entity.getOrgName())
                .orgType(entity.getOrgType())
                .parentId(entity.getParentId())
                .status(entity.getStatus())
                .build();
    }
}
