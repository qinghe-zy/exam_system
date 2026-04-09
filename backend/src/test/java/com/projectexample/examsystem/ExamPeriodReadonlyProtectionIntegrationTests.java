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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ExamPeriodReadonlyProtectionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBlockKeyBackendWritesWhenAnExamIsInProgress() throws Exception {
        String teacherToken = login("800001", "123456", "readonly-teacher-device");
        String adminToken = login("900001", "123456", "readonly-admin-device");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String startTime = LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"READONLY-%s",
                                  "examName":"后台只读保护验证-%s",
                                  "paperId":1,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":60,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"LOCK2026",
                                  "lateEntryMinutes":180,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"STRICT",
                                  "instructionText":"后台只读保护验证",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, startTime, endTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/system/organizations")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgCode":"ORG-LOCK-%s",
                                  "orgName":"考试期组织-%s",
                                  "orgType":"CLASS",
                                  "parentId":1,
                                  "status":1
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"readonly_user_%s",
                                  "nickname":"考试期用户",
                                  "fullName":"考试期用户",
                                  "roleCode":"TEACHER",
                                  "organizationId":1,
                                  "departmentName":"教务处",
                                  "email":"readonly@example.com",
                                  "phone":"13800000000",
                                  "candidateNo":"R-%s",
                                  "password":"ChangeMe123!",
                                  "status":1
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));

        mockMvc.perform(post("/api/exam/questions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionCode":"Q-LOCK-%s",
                                  "subject":"语文",
                                  "questionType":"SHORT_ANSWER",
                                  "difficultyLevel":"MEDIUM",
                                  "stem":"<p>考试期题目保护验证</p>",
                                  "stemHtml":"<p>考试期题目保护验证</p>",
                                  "materialContent":"",
                                  "attachmentJson":"",
                                  "optionsJson":"[]",
                                  "answerKey":"示例答案",
                                  "analysisText":"示例解析",
                                  "knowledgePoint":"考试治理",
                                  "chapterName":"阶段验证",
                                  "sourceName":"自动化",
                                  "tags":"readonly,guard",
                                  "defaultScore":10,
                                  "reviewerStatus":"APPROVED",
                                  "versionNo":1,
                                  "status":1
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"考试期公告-%s",
                                  "category":"SYSTEM",
                                  "status":1,
                                  "content":"考试期公告保护验证"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));

        mockMvc.perform(post("/api/notifications/templates")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode":"EXAM_LOCK_%s",
                                  "templateName":"考试期模板-%s",
                                  "businessType":"EXAM_PUBLISH",
                                  "channelType":"IN_APP",
                                  "titleTemplate":"考试期模板",
                                  "contentTemplate":"考试期模板内容",
                                  "status":1
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
