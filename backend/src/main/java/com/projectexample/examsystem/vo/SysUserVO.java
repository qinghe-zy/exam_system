package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SysUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String fullName;
    private String roleCode;
    private String organizationName;
    private String departmentName;
    private String email;
    private Integer status;
}
