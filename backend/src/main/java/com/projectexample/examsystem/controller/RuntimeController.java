package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.vo.RuntimeHealthVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/system/runtime")
@RequiredArgsConstructor
public class RuntimeController {

    private final Environment environment;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<RuntimeHealthVO> health() throws Exception {
        String productName;
        String productVersion;
        try (Connection connection = dataSource.getConnection()) {
            productName = connection.getMetaData().getDatabaseProductName();
            productVersion = connection.getMetaData().getDatabaseProductVersion();
        }
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return ApiResponse.success(RuntimeHealthVO.builder()
                .applicationName(environment.getProperty("spring.application.name", "exam-system-backend"))
                .activeProfiles(Arrays.asList(environment.getActiveProfiles()))
                .databaseProduct(productName)
                .databaseVersion(productVersion)
                .dbReachable(1)
                .checkedAt(LocalDateTime.now())
                .build());
    }
}
