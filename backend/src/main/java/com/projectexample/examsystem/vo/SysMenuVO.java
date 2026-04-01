package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class SysMenuVO {

    private Long id;
    private String name;
    private String path;
    private String component;
    private String icon;
    private String permissionCode;
    private Long parentId;
    private Integer sortNo;
    private String menuType;

    @Builder.Default
    private List<SysMenuVO> children = new ArrayList<>();
}
