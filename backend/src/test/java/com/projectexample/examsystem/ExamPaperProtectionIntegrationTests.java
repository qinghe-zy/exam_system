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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ExamPaperProtectionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBlockPaperUpdateAndDeleteWhenStartedExamUsesIt() throws Exception {
        String teacherToken = login("800001", "123456");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String startTime = LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"PAPER-%s",
                                  "examName":"试卷保护验证-%s",
                                  "paperId":1,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":60,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"PAPER2026",
                                  "lateEntryMinutes":180,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"STRICT",
                                  "instructionText":"试卷保护验证",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, startTime, endTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/api/exam/papers/1")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paperCode":"SJ-YW-001",
                                  "paperName":"语文期中模拟卷-更新",
                                  "subject":"语文",
                                  "assemblyMode":"MANUAL",
                                  "descriptionText":"试卷保护验证更新",
                                  "paperVersion":"2026春季A卷",
                                  "remarkText":"更新测试",
                                  "durationMinutes":90,
                                  "totalScore":175,
                                  "passScore":60,
                                  "shuffleEnabled":0,
                                  "publishStatus":1,
                                  "questionItems":[
                                    {"questionId":1,"sortNo":1,"score":5,"requiredFlag":1}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));

        mockMvc.perform(delete("/api/exam/papers/1")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "paper-protection-device")
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
