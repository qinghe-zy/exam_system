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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PermissionMatrixIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldEnforceRoleBasedAccessBoundaries() throws Exception {
        String admin = login("900001", "123456");
        String orgAdmin = login("900002", "123456");
        String teacher = login("800001", "123456");
        String grader = login("810001", "123456");
        String student = login("20260001", "123456");

        mockMvc.perform(get("/api/system/organizations").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        MvcResult orgScoped = mockMvc.perform(get("/api/system/organizations").header("Authorization", bearer(orgAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode orgScopedPayload = objectMapper.readTree(orgScoped.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(orgScopedPayload.path("data")).hasSize(1);
        assertThat(orgScopedPayload.path("data").get(0).path("orgName").asText()).isEqualTo("清河大学");
        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(teacher)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exam/questions").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exam/papers").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exam/records").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exam/grading/tasks").header("Authorization", bearer(grader)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exam/candidate/my-exams").header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exam/records/my").header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exam/score-appeals/my/1").header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exam/records/my/wrong-book").header("Authorization", bearer(student)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exam/score-appeals").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system/login-risks").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exam/proctor/events").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system/organizations").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system/runtime/health").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exam/analytics/overview").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
