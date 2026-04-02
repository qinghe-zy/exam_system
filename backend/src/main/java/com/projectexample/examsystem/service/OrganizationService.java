package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.OrganizationSaveRequest;
import com.projectexample.examsystem.vo.OrganizationVO;

import java.util.List;

public interface OrganizationService {

    List<OrganizationVO> listOrganizations();

    OrganizationVO createOrganization(OrganizationSaveRequest request);

    OrganizationVO updateOrganization(Long id, OrganizationSaveRequest request);

    void deleteOrganization(Long id);
}
