package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthRegisterOptionVO {

    private Long organizationId;
    private String organizationName;
    private String organizationType;
}
