package com.projectexample.examsystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ExamRoomSeatExportIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAssignRoomSeatAndExportSignInSheet() throws Exception {
        String teacherToken = login("800001", "123456", "room-seat-teacher");
        String studentToken = login("20260001", "123456", "room-seat-student");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String startTime = LocalDateTime.now().plusMinutes(20).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MvcResult createdPlan = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"ROOM-%s",
                                  "examName":"考场座位导出验证-%s",
                                  "examMode":"NORMAL",
                                  "batchLabel":"第二批次",
                                  "examRoom":"Room-305",
                                  "paperId":1,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":60,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"ROOM2026",
                                  "lateEntryMinutes":60,
                                  "signInRequired":1,
                                  "signInStartMinutes":30,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"BASIC",
                                  "instructionText":"用于验证考场、座位和签到名单导出。",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15,21]
                                }
                                """.formatted(suffix, suffix, startTime, endTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.examRoom").value("Room-305"))
                .andReturn();
        long examPlanId = objectMapper.readTree(createdPlan.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult myExams = mockMvc.perform(get("/api/exam/candidate/my-exams")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode myExamNode = findByExamPlanId(objectMapper.readTree(myExams.getResponse().getContentAsString()).path("data"), examPlanId);
        assertThat(myExamNode).isNotNull();
        assertThat(myExamNode.path("examRoom").asText()).isEqualTo("Room-305");
        assertThat(myExamNode.path("seatNo").asText()).isEqualTo("A01");

        mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId + "/admission-ticket")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.examRoom").value("Room-305"))
                .andExpect(jsonPath("$.data.seatNo").value("A01"));

        MvcResult exported = mockMvc.perform(get("/api/exam/plans/" + examPlanId + "/sign-in-sheet/export")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();
        String csv = exported.getResponse().getContentAsString();
        assertThat(csv).contains("Room-305");
        assertThat(csv).contains("A01");
        assertThat(csv).contains("A02");
        assertThat(csv).contains("EX7-15");
    }

    private JsonNode findByExamPlanId(JsonNode items, long examPlanId) {
        for (JsonNode item : items) {
            if (item.path("examPlanId").asLong() == examPlanId) {
                return item;
            }
        }
        return null;
    }

    private String login(String username, String password, String fingerprint) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .header("X-Device-Fingerprint", fingerprint)
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900")
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
