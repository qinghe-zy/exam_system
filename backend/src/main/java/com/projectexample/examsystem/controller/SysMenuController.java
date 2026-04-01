package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.security.UserPrincipal;
import com.projectexample.examsystem.service.SysMenuService;
import com.projectexample.examsystem.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping
    public ApiResponse<List<SysMenuVO>> list() {
        return ApiResponse.success(sysMenuService.listMenus());
    }

    @GetMapping("/current")
    public ApiResponse<List<SysMenuVO>> current(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(sysMenuService.listCurrentMenus(userPrincipal.getRoleCode()));
    }
}
