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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLifecycleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterAndResetStudentAccountWithMockVerificationCode() throws Exception {
        mockMvc.perform(get("/api/auth/register-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        String registerCode = sendCode("""
                {
                  "purpose":"REGISTER",
                  "channel":"EMAIL",
                  "targetValue":"new.student@example.local",
                  "organizationId":7
                }
                """);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"reg20260403",
                                  "fullName":"注册学生",
                                  "organizationId":7,
                                  "departmentName":"汉语言文学一班",
                                  "email":"new.student@example.local",
                                  "password":"Reg123456!",
                                  "verificationCode":"%s",
                                  "verificationChannel":"EMAIL"
                                }
                                """.formatted(registerCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String token = login("reg20260403", "Reg123456!");
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "risk-device-1")
                        .header("X-Device-Info", "UA=JUnit | Screen=1280x720")
                        .content("""
                                {"username":"reg20260403","password":"wrong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));

        String resetCode = sendCode("""
                {
                  "purpose":"RESET_PASSWORD",
                  "channel":"EMAIL",
                  "username":"reg20260403",
                  "targetValue":"ignored@example.local"
                }
                """);

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"reg20260403",
                                  "verificationCode":"%s",
                                  "verificationChannel":"EMAIL",
                                  "newPassword":"Reset123456!"
                                }
                                """.formatted(resetCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String resetToken = login("reg20260403", "Reset123456!");
        assertThat(resetToken).isNotBlank();

        String adminToken = login("900001", "123456");
        mockMvc.perform(get("/api/system/login-risks")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
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

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "junit-device-1")
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
