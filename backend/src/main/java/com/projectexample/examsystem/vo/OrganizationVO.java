package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrganizationVO {

    private Long id;
    private String orgCode;
    private String orgName;
    private String orgType;
    private Long parentId;
    private Integer status;
    private List<OrganizationVO> children;
}
