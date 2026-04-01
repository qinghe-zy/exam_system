package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.SysMenuVO;

import java.util.List;

public interface SysMenuService {

    List<SysMenuVO> listMenus();

    List<SysMenuVO> listCurrentMenus(String roleCode);
}
