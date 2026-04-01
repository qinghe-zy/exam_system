package com.projectexample.examsystem.infra.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiGatewayProperties {

    private String apiBaseUrl;
    private String apiKey;
    private String model;
}
