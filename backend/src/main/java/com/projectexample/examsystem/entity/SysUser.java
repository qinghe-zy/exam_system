package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String fullName;
    private String roleCode;
    private Long organizationId;
    private String organizationName;
    private String departmentName;
    private String email;
    private String phone;
    private String candidateNo;
    private Integer sessionVersion;
    private Integer loginFailCount;
    private LocalDateTime lastLoginFailureAt;
    private LocalDateTime lockUntil;
    private Integer status;
}
