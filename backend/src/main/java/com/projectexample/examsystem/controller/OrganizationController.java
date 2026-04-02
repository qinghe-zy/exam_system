package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.OrganizationSaveRequest;
import com.projectexample.examsystem.service.OrganizationService;
import com.projectexample.examsystem.vo.OrganizationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<List<OrganizationVO>> list() {
        return ApiResponse.success(organizationService.listOrganizations());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<OrganizationVO> create(@Valid @RequestBody OrganizationSaveRequest request) {
        return ApiResponse.success("organization created", organizationService.createOrganization(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<OrganizationVO> update(@PathVariable Long id, @Valid @RequestBody OrganizationSaveRequest request) {
        return ApiResponse.success("organization updated", organizationService.updateOrganization(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ApiResponse.success("organization deleted", null);
    }
}
