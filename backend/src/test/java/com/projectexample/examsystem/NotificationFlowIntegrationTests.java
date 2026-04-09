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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportNotificationTemplateCrudAndUpcomingReminderDispatch() throws Exception {
        String adminToken = login("900001", "123456");
        String teacherToken = login("800001", "123456");
        String studentToken = login("20260001", "123456");

        mockMvc.perform(get("/api/notifications/templates")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)));

        String suffix = String.valueOf(System.currentTimeMillis()).substring(6);
        MvcResult createdTemplate = mockMvc.perform(post("/api/notifications/templates")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode":"SECURITY_ALERT_SMS_%s",
                                  "templateName":"安全告警短信模板-%s",
                                  "businessType":"SECURITY_ALERT",
                                  "channelType":"MOCK_SMS",
                                  "titleTemplate":"安全告警",
                                  "contentTemplate":"{{content}}",
                                  "status":0
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long templateId = objectMapper.readTree(createdTemplate.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/api/notifications/templates/" + templateId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode":"SECURITY_ALERT_SMS_%s",
                                  "templateName":"安全告警短信模板-%s-更新",
                                  "businessType":"SECURITY_ALERT",
                                  "channelType":"MOCK_SMS",
                                  "titleTemplate":"安全告警",
                                  "contentTemplate":"{{content}}",
                                  "status":0
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.templateName").value("安全告警短信模板-%s-更新".formatted(suffix)));

        MvcResult papersResult = mockMvc.perform(get("/api/exam/papers")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode papers = objectMapper.readTree(papersResult.getResponse().getContentAsString()).path("data");
        long paperId = papers.get(0).path("id").asLong();
        double passScore = Math.min(60D, papers.get(0).path("totalScore").asDouble());

        String startTime = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MvcResult createdPlan = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"NF-%s",
                                  "examName":"通知能力验证-%s",
                                  "paperId":%s,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":%s,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"NF2026",
                                  "lateEntryMinutes":20,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"BASIC",
                                  "instructionText":"用于验证通知模板与开考前提醒链路。",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, paperId, startTime, endTime, passScore)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long examPlanId = objectMapper.readTree(createdPlan.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/notifications/exam-reminders/dispatch")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        MvcResult logsResult = mockMvc.perform(get("/api/notifications/delivery-logs")
                        .queryParam("businessType", "EXAM_REMINDER")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode logs = objectMapper.readTree(logsResult.getResponse().getContentAsString()).path("data").path("records");
        assertThat(logs)
                .anySatisfy(item -> {
                    assertThat(item.path("relatedId").asLong()).isEqualTo(examPlanId);
                    assertThat(item.path("channelType").asText()).isIn("IN_APP", "MOCK_SMS");
                });

        MvcResult messagesResult = mockMvc.perform(get("/api/messages/my")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode messages = objectMapper.readTree(messagesResult.getResponse().getContentAsString()).path("data");
        assertThat(messages)
                .anySatisfy(item -> {
                    assertThat(item.path("messageType").asText()).isEqualTo("EXAM_REMINDER");
                    assertThat(item.path("relatedId").asLong()).isEqualTo(examPlanId);
                });
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "notification-flow-device")
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
