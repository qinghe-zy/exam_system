package com.projectexample.examsystem.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectexample.examsystem.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class AiGatewayClient {

    private final AiGatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiGatewayClient(AiGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getApiBaseUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new BusinessException(4005, "AI 功能尚未配置。请在本地环境变量中设置 AI_API_KEY，并确认 AI_API_BASE_URL / AI_MODEL 可用后重试。");
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getModel(),
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveChatCompletionUrl()))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(5002, "DeepSeek 调用失败，HTTP 状态码：" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || !StringUtils.hasText(contentNode.asText())) {
                throw new BusinessException(5002, "DeepSeek 返回为空，无法生成 AI 建议");
            }
            return contentNode.asText();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(5002, "调用 DeepSeek 时被中断，请稍后重试");
        } catch (IOException exception) {
            throw new BusinessException(5002, "调用 DeepSeek 时发生异常：" + exception.getMessage());
        }
    }

    private String resolveChatCompletionUrl() {
        String apiBaseUrl = properties.getApiBaseUrl().trim();
        if (apiBaseUrl.endsWith("/chat/completions")) {
            return apiBaseUrl;
        }
        if (apiBaseUrl.endsWith("/")) {
            return apiBaseUrl + "chat/completions";
        }
        return apiBaseUrl + "/chat/completions";
    }
}
