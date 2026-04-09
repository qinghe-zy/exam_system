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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ExamPlanModeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportRetakePlanAndExposeModeToStudent() throws Exception {
        String teacherToken = login("800001", "123456");
        String studentToken = login("20260001", "123456");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String startTime = LocalDateTime.now().plusMinutes(20).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MvcResult createdPlanResult = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"RETAKE-%s",
                                  "examName":"语文阶段测验重考-%s",
                                  "examMode":"RETAKE",
                                  "batchLabel":"重考批次 A",
                                  "sourceExamPlanId":1,
                                  "paperId":1,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":60,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":2,
                                  "examPassword":"RT2026",
                                  "lateEntryMinutes":120,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"BASIC",
                                  "instructionText":"用于验证补考 / 重考基础能力。",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, startTime, endTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.examMode").value("RETAKE"))
                .andExpect(jsonPath("$.data.batchLabel").value("重考批次 A"))
                .andExpect(jsonPath("$.data.sourceExamPlanId").value(1))
                .andExpect(jsonPath("$.data.sourceExamName").value("2026级语文阶段测验"))
                .andReturn();
        long examPlanId = objectMapper.readTree(createdPlanResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult plansResult = mockMvc.perform(get("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode plans = objectMapper.readTree(plansResult.getResponse().getContentAsString()).path("data");
        JsonNode createdPlanNode = findById(plans, examPlanId);
        assertThat(createdPlanNode).isNotNull();
        assertThat(createdPlanNode.path("examMode").asText()).isEqualTo("RETAKE");
        assertThat(createdPlanNode.path("batchLabel").asText()).isNotBlank();
        assertThat(createdPlanNode.path("sourceExamPlanId").asLong()).isEqualTo(1L);
        assertThat(createdPlanNode.path("sourceExamName").asText()).isNotBlank();

        MvcResult myExamsResult = mockMvc.perform(get("/api/exam/candidate/my-exams")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode myExams = objectMapper.readTree(myExamsResult.getResponse().getContentAsString()).path("data");
        JsonNode studentExamNode = findByExamPlanId(myExams, examPlanId);
        assertThat(studentExamNode).isNotNull();
        assertThat(studentExamNode.path("examMode").asText()).isEqualTo("RETAKE");
        assertThat(studentExamNode.path("batchLabel").asText()).isNotBlank();
        assertThat(studentExamNode.path("sourceExamPlanId").asLong()).isEqualTo(1L);
        assertThat(studentExamNode.path("sourceExamName").asText()).isNotBlank();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "exam-plan-mode-device")
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900")
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = payload.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode findById(JsonNode items, long id) {
        for (JsonNode item : items) {
            if (item.path("id").asLong() == id) {
                return item;
            }
        }
        return null;
    }

    private JsonNode findByExamPlanId(JsonNode items, long examPlanId) {
        for (JsonNode item : items) {
            if (item.path("examPlanId").asLong() == examPlanId) {
                return item;
            }
        }
        return null;
    }
}
