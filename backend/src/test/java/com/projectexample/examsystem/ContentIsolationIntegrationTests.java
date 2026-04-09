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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ContentIsolationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRestrictNoticeAndTemplateAccessByOrganization() throws Exception {
        String teacherOrg2Token = login("800001", "123456", "org2-content-device");
        String teacherOrg3Token = login("800002", "123456", "org3-content-device");
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);

        MvcResult createdNotice = mockMvc.perform(post("/api/notices")
                        .header("Authorization", bearer(teacherOrg2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"组织隔离公告-%s",
                                  "category":"SYSTEM",
                                  "status":1,
                                  "content":"仅文学院范围可见的公告。"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long noticeId = objectMapper.readTree(createdNotice.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult createdTemplate = mockMvc.perform(post("/api/notifications/templates")
                        .header("Authorization", bearer(teacherOrg2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode":"ORG2_TEMPLATE_%s",
                                  "templateName":"组织隔离模板-%s",
                                  "businessType":"EXAM_PUBLISH",
                                  "channelType":"IN_APP",
                                  "titleTemplate":"组织隔离模板",
                                  "contentTemplate":"仅文学院考试计划使用",
                                  "status":1
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long templateId = objectMapper.readTree(createdTemplate.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult visibleNoticeList = mockMvc.perform(get("/api/notices")
                        .queryParam("pageNum", "1")
                        .queryParam("pageSize", "20")
                        .header("Authorization", bearer(teacherOrg2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode noticeRecords = objectMapper.readTree(visibleNoticeList.getResponse().getContentAsString()).path("data").path("records");
        assertThat(noticeRecords)
                .anySatisfy(item -> assertThat(item.path("id").asLong()).isEqualTo(noticeId));

        MvcResult invisibleNoticeList = mockMvc.perform(get("/api/notices")
                        .queryParam("pageNum", "1")
                        .queryParam("pageSize", "20")
                        .header("Authorization", bearer(teacherOrg3Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode foreignNoticeRecords = objectMapper.readTree(invisibleNoticeList.getResponse().getContentAsString()).path("data").path("records");
        assertThat(foreignNoticeRecords)
                .noneSatisfy(item -> assertThat(item.path("id").asLong()).isEqualTo(noticeId));

        mockMvc.perform(get("/api/notices/" + noticeId)
                        .header("Authorization", bearer(teacherOrg3Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4031));

        MvcResult visibleTemplateList = mockMvc.perform(get("/api/notifications/templates")
                        .header("Authorization", bearer(teacherOrg2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode templateItems = objectMapper.readTree(visibleTemplateList.getResponse().getContentAsString()).path("data");
        assertThat(templateItems)
                .anySatisfy(item -> assertThat(item.path("id").asLong()).isEqualTo(templateId));

        MvcResult invisibleTemplateList = mockMvc.perform(get("/api/notifications/templates")
                        .header("Authorization", bearer(teacherOrg3Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode foreignTemplateItems = objectMapper.readTree(invisibleTemplateList.getResponse().getContentAsString()).path("data");
        assertThat(foreignTemplateItems)
                .noneSatisfy(item -> assertThat(item.path("id").asLong()).isEqualTo(templateId));

        mockMvc.perform(put("/api/notifications/templates/" + templateId)
                        .header("Authorization", bearer(teacherOrg3Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode":"ORG2_TEMPLATE_%s",
                                  "templateName":"越权更新",
                                  "businessType":"EXAM_PUBLISH",
                                  "channelType":"IN_APP",
                                  "titleTemplate":"越权更新",
                                  "contentTemplate":"不应被其他组织修改",
                                  "status":1
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4031));
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
