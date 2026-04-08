package com.projectexample.examsystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLockAccountThenAllowResetPasswordAndNotifyAdmins() throws Exception {
        String targetUsername = "20260002";
        String attackIp = "10.20.30.40";

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteIp(attackIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Device-Fingerprint", "security-device-lock")
                            .header("X-Device-Info", "UA=JUnit | Screen=1280x720")
                            .content("""
                                    {"username":"%s","password":"wrong-password"}
                                    """.formatted(targetUsername)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteIp(attackIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "security-device-lock")
                        .header("X-Device-Info", "UA=JUnit | Screen=1280x720")
                        .content("""
                                {"username":"%s","password":"123456"}
                                """.formatted(targetUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4013));

        String resetCode = sendCode("""
                {
                  "purpose":"RESET_PASSWORD",
                  "channel":"SMS",
                  "username":"20260002",
                  "targetValue":"ignored"
                }
                """);

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"20260002",
                                  "verificationCode":"%s",
                                  "verificationChannel":"SMS",
                                  "newPassword":"Reset2026!"
                                }
                                """.formatted(resetCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String resetToken = login("20260002", "Reset2026!", "10.20.30.41");
        assertThat(resetToken).isNotBlank();

        String adminToken = login("900001", "123456", "127.0.0.1");
        MvcResult adminMessages = mockMvc.perform(get("/api/messages/my")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode messagePayload = objectMapper.readTree(adminMessages.getResponse().getContentAsString()).path("data");
        assertThat(messagePayload)
                .anySatisfy(item -> {
                    assertThat(item.path("messageType").asText()).isEqualTo("SECURITY_ALERT");
                    assertThat(item.path("content").asText()).contains("20260002");
                });
    }

    @Test
    void shouldRateLimitBurstLoginRequestsFromSameIp() throws Exception {
        String burstIp = "10.20.30.88";
        for (int attempt = 0; attempt < 12; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteIp(burstIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"ghost-user-%s","password":"wrong-password"}
                                    """.formatted(attempt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteIp(burstIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ghost-user-final","password":"wrong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4012))
                .andExpect(jsonPath("$.message").value("当前 IP 登录过于频繁，请稍后再试"));
    }

    @Test
    void shouldThrottleVerificationCodeRequestsForSameTarget() throws Exception {
        mockMvc.perform(post("/api/auth/verification-codes/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose":"REGISTER",
                                  "channel":"EMAIL",
                                  "targetValue":"security.throttle@example.local",
                                  "organizationId":7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/verification-codes/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose":"REGISTER",
                                  "channel":"EMAIL",
                                  "targetValue":"security.throttle@example.local",
                                  "organizationId":7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4014))
                .andExpect(jsonPath("$.message").value("验证码发送过于频繁，请稍后再试"));
    }

    private String sendCode(String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/verification-codes/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("mockCode").asText();
    }

    private String login(String username, String password, String ip) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(remoteIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "security-device-" + username)
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900")
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("token").asText();
    }

    private RequestPostProcessor remoteIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
