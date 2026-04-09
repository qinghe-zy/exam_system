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
class ApiSmokeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoadCoreExamChainApis() throws Exception {
        String adminToken = login("900001", "123456");
        String studentToken = login("20260001", "123456");

        mockMvc.perform(get("/api/system/organizations")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].orgName").value("清河大学"));

        mockMvc.perform(get("/api/system/config-center/configs")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/system/runtime/health")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dbReachable").value(1));

        mockMvc.perform(get("/api/system/login-risks")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/exam/questions/export")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult exportedRecords = mockMvc.perform(get("/api/exam/records/export")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(exportedRecords.getResponse().getContentType()).contains("text/csv");

        mockMvc.perform(get("/api/messages/my")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult myScores = mockMvc.perform(get("/api/exam/records/my")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode myScoresPayload = objectMapper.readTree(myScores.getResponse().getContentAsString());
        long publishedScoreId = myScoresPayload.path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/exam/records/my/" + publishedScoreId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/exam/score-appeals/my/" + publishedScoreId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/exam/records/my/wrong-book")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/exam/candidate/my-exams")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/exam/candidate/exams/1/admission-ticket")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.examPlanId").value(1));

        mockMvc.perform(get("/api/exam/candidate/exams/1")
                        .queryParam("examPassword", "YW2026")
                        .header("X-Device-Fingerprint", "smoke-device-a")
                        .header("X-Device-Info", "UA=Smoke | Screen=1440x900")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.antiCheatLevel").value("BASIC"))
                .andExpect(jsonPath("$.data.antiCheatPolicy.deviceLoggingEnabled").value(1))
                .andExpect(jsonPath("$.data.antiCheatPolicy.deviceCheckEnabled").value(0))
                .andExpect(jsonPath("$.data.antiCheatPolicy.blockedShortcutKeys.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/exam/proctor/events")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].eventType").isNotEmpty());

        mockMvc.perform(get("/api/exam/score-appeals")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/exam/analytics/overview")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scoreBands.length()").value(4))
                .andExpect(jsonPath("$.data.organizationComparisons.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.trendPoints.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.excellentRate").exists());

        mockMvc.perform(get("/api/exam/analytics/quality-report")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dimensionScores.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.recommendations.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult exportedAnalysis = mockMvc.perform(get("/api/exam/analytics/export")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(exportedAnalysis.getResponse().getContentType()).contains("text/csv");

        MvcResult exportedQualityReport = mockMvc.perform(get("/api/exam/analytics/quality-report/export")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(exportedQualityReport.getResponse().getContentType()).contains("text/markdown");
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
        String token = payload.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
