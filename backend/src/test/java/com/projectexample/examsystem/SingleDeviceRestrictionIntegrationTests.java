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
class SingleDeviceRestrictionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBlockSecondDeviceWhenStrictExamHasExistingDeviceContext() throws Exception {
        String teacherToken = login("800001", "123456");
        String studentToken = login("20260001", "123456");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String startTime = LocalDateTime.now().minusMinutes(5).format(formatter);
        String endTime = LocalDateTime.now().plusHours(2).format(formatter);

        MvcResult createdPlanResult = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"STRICT-%s",
                                  "examName":"单设备限制验证-%s",
                                  "paperId":1,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":60,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"STRICT2026",
                                  "lateEntryMinutes":180,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"STRICT",
                                  "instructionText":"单设备限制验证",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, startTime, endTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode createdPlanPayload = objectMapper.readTree(createdPlanResult.getResponse().getContentAsString());
        long examPlanId = createdPlanPayload.path("data").path("id").asLong();

        MvcResult workspaceResult = mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId)
                        .queryParam("examPassword", "STRICT2026")
                        .header("X-Device-Fingerprint", "strict-device-a")
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode workspacePayload = objectMapper.readTree(workspaceResult.getResponse().getContentAsString());
        long answerSheetId = workspacePayload.path("data").path("answerSheetId").asLong();

        mockMvc.perform(post("/api/exam/candidate/exams/" + examPlanId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("""
                                {
                                  "answerSheetId":%s,
                                  "eventType":"DEVICE_CONTEXT",
                                  "severity":"LOW",
                                  "deviceFingerprint":"strict-device-a",
                                  "deviceInfo":"UA=JUnit | Screen=1440x900",
                                  "detailText":"integration strict device context"
                                }
                                """.formatted(answerSheetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId)
                        .queryParam("examPassword", "STRICT2026")
                        .header("X-Device-Fingerprint", "strict-device-b")
                        .header("X-Device-Info", "UA=JUnit | Screen=1024x768")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.90.0." + ("800001".equals(username) ? "1" : "2"));
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "junit-auth-device")
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
}
