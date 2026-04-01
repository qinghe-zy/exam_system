package com.projectexample.examsystem.service;

import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.vo.SysUserVO;

import java.util.List;

public interface SysUserService {

    SysUser findByUsername(String username);

    List<SysUserVO> listUsers();
}
