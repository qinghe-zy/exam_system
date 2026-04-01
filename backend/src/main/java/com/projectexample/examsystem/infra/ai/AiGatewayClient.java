package com.projectexample.examsystem.infra.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiGatewayClient {

    private final AiGatewayProperties properties;

    public AiGatewayClient(AiGatewayProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getApiBaseUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }
}
