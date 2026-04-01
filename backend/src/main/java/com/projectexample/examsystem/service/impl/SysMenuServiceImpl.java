package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.SysMenu;
import com.projectexample.examsystem.mapper.SysMenuMapper;
import com.projectexample.examsystem.service.SysMenuService;
import com.projectexample.examsystem.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenuVO> listMenus() {
        return buildTree(sysMenuMapper.selectList(Wrappers.lambdaQuery(SysMenu.class)
                .orderByAsc(SysMenu::getSortNo, SysMenu::getId)));
    }

    @Override
    public List<SysMenuVO> listCurrentMenus(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        return buildTree(sysMenuMapper.selectList(Wrappers.lambdaQuery(SysMenu.class)
                        .orderByAsc(SysMenu::getSortNo, SysMenu::getId))
                .stream()
                .filter(menu -> supportsRole(menu.getVisibleRoles(), roleCode))
                .toList());
    }

    private List<SysMenuVO> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenuVO>> grouped = menus.stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(SysMenuVO::getSortNo).thenComparing(SysMenuVO::getId))
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));

        List<SysMenuVO> roots = grouped.getOrDefault(0L, List.of());
        roots.forEach(root -> attachChildren(root, grouped));
        return roots;
    }

    private void attachChildren(SysMenuVO node, Map<Long, List<SysMenuVO>> grouped) {
        List<SysMenuVO> children = grouped.getOrDefault(node.getId(), List.of());
        node.setChildren(children);
        children.forEach(child -> attachChildren(child, grouped));
    }

    private SysMenuVO toVO(SysMenu menu) {
        return SysMenuVO.builder()
                .id(menu.getId())
                .name(menu.getName())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .permissionCode(menu.getPermissionCode())
                .parentId(menu.getParentId())
                .sortNo(menu.getSortNo())
                .menuType(menu.getMenuType())
                .build();
    }

    private boolean supportsRole(String visibleRoles, String roleCode) {
        if (!StringUtils.hasText(visibleRoles)) {
            return true;
        }
        return List.of(visibleRoles.split(",")).stream()
                .map(String::trim)
                .anyMatch(item -> item.equalsIgnoreCase(roleCode));
    }
}
