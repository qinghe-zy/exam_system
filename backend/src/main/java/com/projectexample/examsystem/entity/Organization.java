package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("biz_organization")
@EqualsAndHashCode(callSuper = true)
public class Organization extends BaseEntity {

    private String orgCode;
    private String orgName;
    private String orgType;

    @TableField("parent_id")
    private Long parentId;

    private Integer status;
}
