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
class GradingGovernanceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportReviewAppealAndRejudgeFlow() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String startTime = LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String teacherToken = login("800001", "123456");
        String graderToken = login("810001", "123456");
        String studentToken = login("20260001", "123456");
        String adminToken = login("900001", "123456");

        MvcResult papersResult = mockMvc.perform(get("/api/exam/papers")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode papers = objectMapper.readTree(papersResult.getResponse().getContentAsString()).path("data");
        long paperId = papers.get(0).path("id").asLong();
        double passScore = Math.min(60D, papers.get(0).path("totalScore").asDouble());

        MvcResult createPlan = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"GG-%s",
                                  "examName":"阅卷治理验证-%s",
                                  "paperId":%s,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":90,
                                  "passScore":%s,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"GG2026",
                                  "lateEntryMinutes":240,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"STRICT",
                                  "instructionText":"用于验证复核与申诉治理流程。",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, paperId, startTime, endTime, passScore)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long examPlanId = objectMapper.readTree(createPlan.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult workspaceResult = mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId)
                        .queryParam("examPassword", "GG2026")
                        .header("Authorization", bearer(studentToken))
                        .header("X-Device-Fingerprint", "governance-device-a")
                        .header("X-Device-Info", "UA=JUnit | Screen=1440x900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode workspace = objectMapper.readTree(workspaceResult.getResponse().getContentAsString()).path("data");
        JsonNode items = workspace.path("items");
        String answersPayload = """
                {
                  "answers":[
                    {"questionId":%s,"answerContent":"选项一：符合教材结论"},
                    {"questionId":%s,"answerContent":"说法一：符合教材结论|说法三：符合题意要求"},
                    {"questionId":%s,"answerContent":"正确"},
                    {"questionId":%s,"answerContent":"这是第一次提交的主观题答案。"}
                  ]
                }
                """.formatted(
                items.get(0).path("questionId").asLong(),
                items.get(1).path("questionId").asLong(),
                items.get(2).path("questionId").asLong(),
                items.get(3).path("questionId").asLong()
        );

        mockMvc.perform(post("/api/exam/candidate/exams/" + examPlanId + "/submit")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult tasksResult = mockMvc.perform(get("/api/exam/grading/tasks")
                        .header("Authorization", bearer(graderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode tasks = objectMapper.readTree(tasksResult.getResponse().getContentAsString()).path("data");
        long answerSheetId = tasks.findValues("answerSheetId").stream()
                .mapToLong(JsonNode::asLong)
                .max()
                .orElseThrow();

        MvcResult gradingWorkspace = mockMvc.perform(get("/api/exam/grading/" + answerSheetId)
                        .header("Authorization", bearer(graderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode gradeItems = objectMapper.readTree(gradingWorkspace.getResponse().getContentAsString()).path("data").path("items");
        String initialGradePayload = buildGradePayload(gradeItems, 8, "第一次评分结果。");

        mockMvc.perform(post("/api/exam/grading/" + answerSheetId + "/submit")
                        .header("Authorization", bearer(graderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialGradePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REVIEW_PENDING"))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING"));

        mockMvc.perform(post("/api/exam/grading/" + answerSheetId + "/review")
                        .header("Authorization", bearer(graderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action":"APPROVE",
                                  "reviewComment":"复核通过。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"));

        MvcResult myScores = mockMvc.perform(get("/api/exam/records/my")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode scoreRecords = objectMapper.readTree(myScores.getResponse().getContentAsString()).path("data");
        long scoreRecordId = scoreRecords.findValues("id").stream().mapToLong(JsonNode::asLong).max().orElseThrow();

        mockMvc.perform(post("/api/exam/score-appeals/my/" + scoreRecordId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appealReason":"我认为主观题得分偏低，请求复核。",
                                  "expectedOutcome":"重新核对主观题评分"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        MvcResult appealList = mockMvc.perform(get("/api/exam/score-appeals")
                        .queryParam("scoreRecordId", String.valueOf(scoreRecordId))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long appealId = objectMapper.readTree(appealList.getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        mockMvc.perform(post("/api/exam/score-appeals/" + appealId + "/process")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action":"REJUDGE",
                                  "processComment":"同意转入重判。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED_REJUDGE"));

        mockMvc.perform(post("/api/exam/grading/" + answerSheetId + "/submit")
                        .header("Authorization", bearer(graderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildGradePayload(gradeItems, 12, "重判后调整得分。")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REVIEW_PENDING"));

        mockMvc.perform(post("/api/exam/grading/" + answerSheetId + "/review")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action":"APPROVE",
                                  "reviewComment":"重判复核通过。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.appealStatus").value("RESOLVED"));

        mockMvc.perform(get("/api/exam/records/my/" + scoreRecordId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.subjectiveScore").value(60.0))
                .andExpect(jsonPath("$.data.finalScore").value(80.0))
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.appealStatus").value("RESOLVED"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", "governance-auth-device")
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

    private String buildGradePayload(JsonNode gradeItems, int score, String comment) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"gradeItems\":[");
        boolean first = true;
        for (JsonNode item : gradeItems) {
            String questionType = item.path("questionType").asText();
            if ("SINGLE_CHOICE".equals(questionType) || "MULTIPLE_CHOICE".equals(questionType) || "TRUE_FALSE".equals(questionType)) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append("{\"answerItemId\":")
                    .append(item.path("answerItemId").asLong())
                    .append(",\"scoreAwarded\":")
                    .append(score)
                    .append(",\"reviewComment\":\"")
                    .append(comment)
                    .append("\"}");
            first = false;
        }
        builder.append("]}");
        return builder.toString();
    }
}
