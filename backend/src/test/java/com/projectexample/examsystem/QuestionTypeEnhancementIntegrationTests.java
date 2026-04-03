package com.projectexample.examsystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionTypeEnhancementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnswerItemMapper answerItemMapper;

    @Test
    void shouldSupportFillBlankAndMaterialQuestionChain() throws Exception {
        String teacherToken = login("800001", "123456");
        String studentToken = login("20260001", "123456");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(8);

        long fillBlankQuestionId = createQuestion(teacherToken, """
                {
                  "questionCode":"TK-%s",
                  "subject":"语文",
                  "questionType":"FILL_BLANK",
                  "difficultyLevel":"MEDIUM",
                  "stem":"请按顺序填写两位中国古代科学家。",
                  "stemHtml":"<p>请按顺序填写两位<strong>中国古代科学家</strong>。</p>",
                  "materialContent":"",
                  "attachmentJson":"[]",
                  "optionsJson":"[\\"第1空\\",\\"第2空\\"]",
                  "answerKey":"祖冲之|张衡",
                  "analysisText":"答案必须按顺序填写，且每空之间使用竖线分隔。",
                  "knowledgePoint":"古代科学史",
                  "chapterName":"传统文化",
                  "sourceName":"自动化测试",
                  "tags":"填空题,历史文化",
                  "defaultScore":10,
                  "reviewerStatus":"APPROVED",
                  "versionNo":1,
                  "status":1
                }
                """.formatted(suffix));

        long materialQuestionId = createQuestion(teacherToken, """
                {
                  "questionCode":"CL-%s",
                  "subject":"语文",
                  "questionType":"MATERIAL",
                  "difficultyLevel":"MEDIUM",
                  "stem":"请根据材料概括作者对求学态度的看法。",
                  "stemHtml":"<p>请根据材料概括作者对<em>求学态度</em>的看法。</p>",
                  "materialContent":"<article><h4>材料</h4><p>学而不思则罔，思而不学则殆。</p></article>",
                  "attachmentJson":"[{\\"name\\":\\"参考插图\\",\\"url\\":\\"https://example.com/material.png\\",\\"type\\":\\"image\\"}]",
                  "optionsJson":"[]",
                  "answerKey":"应围绕勤学与善思并重展开作答。",
                  "analysisText":"材料题基础版当前采用“材料正文 + 主问题”模式，不拆子题。",
                  "knowledgePoint":"文言文阅读",
                  "chapterName":"综合阅读",
                  "sourceName":"自动化测试",
                  "tags":"材料题,阅读理解",
                  "defaultScore":15,
                  "reviewerStatus":"APPROVED",
                  "versionNo":1,
                  "status":1
                }
                """.formatted(suffix));

        long paperId = createPaper(teacherToken, fillBlankQuestionId, materialQuestionId, suffix);
        long examPlanId = createExamPlan(teacherToken, paperId, suffix);

        MvcResult workspaceResult = mockMvc.perform(get("/api/exam/candidate/exams/" + examPlanId)
                        .queryParam("examPassword", "QT" + suffix)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].questionType").value("FILL_BLANK"))
                .andExpect(jsonPath("$.data.items[1].questionType").value("MATERIAL"))
                .andExpect(jsonPath("$.data.items[1].materialContent").exists())
                .andReturn();

        JsonNode workspacePayload = objectMapper.readTree(workspaceResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long answerSheetId = workspacePayload.path("data").path("answerSheetId").asLong();

        mockMvc.perform(post("/api/exam/candidate/exams/" + examPlanId + "/submit")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers":[
                                    {"questionId":%s,"answerContent":"祖冲之|张衡"},
                                    {"questionId":%s,"answerContent":"作者强调学习既要勤勉积累，也要主动思考。"}
                                  ]
                                }
                                """.formatted(fillBlankQuestionId, materialQuestionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        AnswerItem fillBlankAnswer = answerItemMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(AnswerItem.class)
                .eq(AnswerItem::getAnswerSheetId, answerSheetId)
                .eq(AnswerItem::getQuestionId, fillBlankQuestionId)
                .last("limit 1"));
        AnswerItem materialAnswer = answerItemMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(AnswerItem.class)
                .eq(AnswerItem::getAnswerSheetId, answerSheetId)
                .eq(AnswerItem::getQuestionId, materialQuestionId)
                .last("limit 1"));

        assertThat(fillBlankAnswer).isNotNull();
        assertThat(fillBlankAnswer.getScoreAwarded()).isEqualTo(10D);
        assertThat(fillBlankAnswer.getStatus()).isEqualTo("AUTO_SCORED");
        assertThat(materialAnswer).isNotNull();
        assertThat(materialAnswer.getStatus()).isEqualTo("PENDING_GRADING");
    }

    private long createQuestion(String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exam/questions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return jsonNode.path("data").path("id").asLong();
    }

    private long createPaper(String token, long fillBlankQuestionId, long materialQuestionId, String suffix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exam/papers")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paperCode":"QP-%s",
                                  "paperName":"题型增强卷-%s",
                                  "subject":"语文",
                                  "assemblyMode":"MANUAL",
                                  "descriptionText":"覆盖填空题和材料题",
                                  "paperVersion":"2026增强版",
                                  "remarkText":"自动化测试",
                                  "durationMinutes":60,
                                  "totalScore":25,
                                  "passScore":15,
                                  "shuffleEnabled":0,
                                  "questionTypeConfigs":[],
                                  "difficultyConfigs":[],
                                  "publishStatus":1,
                                  "questionItems":[
                                    {"questionId":%s,"sortNo":1,"score":10,"requiredFlag":1},
                                    {"questionId":%s,"sortNo":2,"score":15,"requiredFlag":1}
                                  ]
                                }
                                """.formatted(suffix, suffix, fillBlankQuestionId, materialQuestionId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("id").asLong();
    }

    private long createExamPlan(String token, long paperId, String suffix) throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        MvcResult result = mockMvc.perform(post("/api/exam/plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examCode":"QE-%s",
                                  "examName":"题型增强考试-%s",
                                  "paperId":%s,
                                  "startTime":"%s",
                                  "endTime":"%s",
                                  "durationMinutes":60,
                                  "passScore":15,
                                  "candidateScope":"ASSIGNED",
                                  "attemptLimit":1,
                                  "examPassword":"QT%s",
                                  "lateEntryMinutes":30,
                                  "earlySubmitMinutes":0,
                                  "autoSubmitEnabled":1,
                                  "antiCheatLevel":"BASIC",
                                  "instructionText":"请完成题型增强测试",
                                  "status":1,
                                  "publishStatus":1,
                                  "candidateUserIds":[15]
                                }
                                """.formatted(suffix, suffix, paperId, start, end, suffix)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
