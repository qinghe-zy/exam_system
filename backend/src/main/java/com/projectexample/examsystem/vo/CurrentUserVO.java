package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CurrentUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String fullName;
    private String roleCode;
    private String organizationName;
    private List<String> permissions;
}
