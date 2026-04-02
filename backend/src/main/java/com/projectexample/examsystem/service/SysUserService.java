package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.CandidateImportRequest;
import com.projectexample.examsystem.dto.SysUserSaveRequest;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.vo.CandidateImportResultVO;
import com.projectexample.examsystem.vo.SysUserVO;

import java.util.List;

public interface SysUserService {

    SysUser findByUsername(String username);

    List<SysUserVO> listUsers();

    SysUserVO createUser(SysUserSaveRequest request);

    SysUserVO updateUser(Long id, SysUserSaveRequest request);

    CandidateImportResultVO importCandidates(CandidateImportRequest request);
}
