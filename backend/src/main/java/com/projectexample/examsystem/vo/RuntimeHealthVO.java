package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RuntimeHealthVO {

    private String applicationName;
    private List<String> activeProfiles;
    private String databaseProduct;
    private String databaseVersion;
    private Integer dbReachable;
    private LocalDateTime checkedAt;
}
