package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SysRoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private String remark;
}
