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
class ExamCheckInAdmissionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportCheckInAndAdmissionTicketBeforeEnteringExam() throws Exception {
        String teacherToken = login("800001", "123456", "signin-teacher-device");
        String studentToken = login("20260001", "123456", "signin-student-device");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String startTime = LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MvcResult createdPlan = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"CHECKIN-%s",
                                  "examName":"签到规则验证-%s",
                                  "examMode":"NORMAL",
                                  "batchLabel":"签到批次 A",
                                  "paperId":1,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":60,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"QD2026",
                                  "lateEntryMinutes":60,
                                  "signInRequired":1,
                                  "signInStartMinutes":90,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"BASIC",
                                  "instructionText":"请先完成签到，再查看准考证并进入考试。",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, startTime, endTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.signInRequired").value(1))
                .andReturn();
        long examPlanId = objectMapper.readTree(createdPlan.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult myExams = mockMvc.perform(get("/api/exam/candidate/my-exams")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode myExamNode = findByExamPlanId(objectMapper.readTree(myExams.getResponse().getContentAsString()).path("data"), examPlanId);
        assertThat(myExamNode).isNotNull();
        assertThat(myExamNode.path("signInRequired").asInt()).isEqualTo(1);
        assertThat(myExamNode.path("signedInFlag").asInt()).isEqualTo(0);
        assertThat(myExamNode.path("signInOpenAt").asText()).isNotBlank();

        mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId + "/admission-ticket")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.examPlanId").value(examPlanId))
                .andExpect(jsonPath("$.data.accessCode").isNotEmpty())
                .andExpect(jsonPath("$.data.signInRequired").value(1));

        mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId)
                        .queryParam("examPassword", "QD2026")
                        .header("Authorization", bearer(studentToken))
                        .header("X-Device-Fingerprint", "signin-student-device")
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));

        mockMvc.perform(post("/api/exam/candidate/exams/" + examPlanId + "/sign-in")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.signedInFlag").value(1));

        MvcResult teacherPlans = mockMvc.perform(get("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode createdPlanNode = findByPlanId(objectMapper.readTree(teacherPlans.getResponse().getContentAsString()).path("data"), examPlanId);
        assertThat(createdPlanNode).isNotNull();
        assertThat(createdPlanNode.path("signedInCount").asInt()).isEqualTo(1);

        mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId)
                        .queryParam("examPassword", "QD2026")
                        .header("Authorization", bearer(studentToken))
                        .header("X-Device-Fingerprint", "signin-student-device")
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.examPlanId").value(examPlanId));
    }

    private String login(String username, String password, String fingerprint) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", fingerprint)
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

    private JsonNode findByExamPlanId(JsonNode items, long examPlanId) {
        for (JsonNode item : items) {
            if (item.path("examPlanId").asLong() == examPlanId) {
                return item;
            }
        }
        return null;
    }

    private JsonNode findByPlanId(JsonNode items, long examPlanId) {
        for (JsonNode item : items) {
            if (item.path("id").asLong() == examPlanId) {
                return item;
            }
        }
        return null;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
