package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.CandidateImportRequest;
import com.projectexample.examsystem.dto.SysUserSaveRequest;
import com.projectexample.examsystem.service.SysUserService;
import com.projectexample.examsystem.vo.CandidateImportResultVO;
import com.projectexample.examsystem.vo.SysUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<List<SysUserVO>> list() {
        return ApiResponse.success(sysUserService.listUsers());
    }

    @GetMapping("/assignable-candidates")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<SysUserVO>> assignableCandidates() {
        return ApiResponse.success(sysUserService.listAssignableCandidates());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<SysUserVO> create(@Valid @RequestBody SysUserSaveRequest request) {
        return ApiResponse.success("user created", sysUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<SysUserVO> update(@PathVariable Long id, @Valid @RequestBody SysUserSaveRequest request) {
        return ApiResponse.success("user updated", sysUserService.updateUser(id, request));
    }

    @PostMapping("/import-candidates")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<CandidateImportResultVO> importCandidates(@Valid @RequestBody CandidateImportRequest request) {
        return ApiResponse.success("candidates imported", sysUserService.importCandidates(request));
    }
}
