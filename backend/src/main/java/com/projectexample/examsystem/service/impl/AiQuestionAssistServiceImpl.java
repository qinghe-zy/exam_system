package com.projectexample.examsystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectexample.examsystem.dto.AiQuestionDraftRequest;
import com.projectexample.examsystem.dto.AiQuestionPolishRequest;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.infra.ai.AiGatewayClient;
import com.projectexample.examsystem.service.AiQuestionAssistService;
import com.projectexample.examsystem.vo.AiQuestionDraftVO;
import com.projectexample.examsystem.vo.AiQuestionPolishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiQuestionAssistServiceImpl implements AiQuestionAssistService {

    private final AiGatewayClient aiGatewayClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiQuestionDraftVO generateDraft(AiQuestionDraftRequest request) {
        String content = aiGatewayClient.chat(
                "你是一名严谨的中文考试命题助手。请只返回 JSON，不要输出额外说明。",
                """
                请根据以下条件生成一题适合教师继续编辑的候选题目草稿，并返回 JSON。
                必须包含字段：
                stem, optionsJson, answerKey, analysisText, knowledgePoint, chapterName, tags, defaultScore
                规则：
                1. 如果题型是单选/多选/判断题，optionsJson 必须是 JSON 数组字符串
                2. 如果题型是简答题，optionsJson 返回 [] 即可
                3. 内容必须为中文
                4. 不要生成题目编码
                5. 题目要适合在线考试系统直接回填到题库表单

                学科：%s
                题型：%s
                难度：%s
                知识点：%s
                章节：%s
                额外要求：%s
                """.formatted(
                        request.getSubject(),
                        request.getQuestionType(),
                        request.getDifficultyLevel(),
                        request.getKnowledgePoint(),
                        blankToDefault(request.getChapterName(), "未指定"),
                        blankToDefault(request.getExtraRequirements(), "无")
                )
        );
        JsonNode root = parseJsonContent(content);
        return AiQuestionDraftVO.builder()
                .subject(request.getSubject())
                .questionType(request.getQuestionType())
                .difficultyLevel(request.getDifficultyLevel())
                .stem(root.path("stem").asText())
                .optionsJson(asJsonArrayString(root.path("optionsJson").asText("[]")))
                .answerKey(root.path("answerKey").asText())
                .analysisText(root.path("analysisText").asText())
                .knowledgePoint(blankToDefault(root.path("knowledgePoint").asText(), request.getKnowledgePoint()))
                .chapterName(blankToDefault(root.path("chapterName").asText(), request.getChapterName()))
                .tags(root.path("tags").asText())
                .defaultScore(root.path("defaultScore").isNumber() ? root.path("defaultScore").asDouble() : defaultScore(request.getQuestionType()))
                .aiHint("以下内容为 AI 辅助生成草稿，请教师复核后再保存入库。")
                .build();
    }

    @Override
    public AiQuestionPolishVO polishQuestion(AiQuestionPolishRequest request) {
        String content = aiGatewayClient.chat(
                "你是一名中文考试题库编辑助手。请只返回 JSON，不要输出额外说明。",
                """
                请基于以下题目信息给出“AI 辅助优化”结果，并返回 JSON。
                必须包含字段：
                improvedStem, improvedAnswerKey, improvedAnalysisText, suggestedOptionsJson
                规则：
                1. 保持学科、题型和难度一致
                2. 题干更清晰，解析更适合教师审阅
                3. 如果原题型是主观题，suggestedOptionsJson 返回 []
                4. 如果原题型是客观题，可在保留原意的前提下优化选项文字

                学科：%s
                题型：%s
                难度：%s
                知识点：%s
                章节：%s
                当前题干：%s
                当前选项：%s
                当前答案：%s
                当前解析：%s
                """.formatted(
                        request.getSubject(),
                        request.getQuestionType(),
                        request.getDifficultyLevel(),
                        blankToDefault(request.getKnowledgePoint(), "未填写"),
                        blankToDefault(request.getChapterName(), "未填写"),
                        request.getStem(),
                        blankToDefault(request.getOptionsJson(), "[]"),
                        blankToDefault(request.getAnswerKey(), "未填写"),
                        blankToDefault(request.getAnalysisText(), "未填写")
                )
        );
        JsonNode root = parseJsonContent(content);
        return AiQuestionPolishVO.builder()
                .improvedStem(root.path("improvedStem").asText(request.getStem()))
                .improvedAnswerKey(root.path("improvedAnswerKey").asText(request.getAnswerKey()))
                .improvedAnalysisText(root.path("improvedAnalysisText").asText(request.getAnalysisText()))
                .suggestedOptionsJson(asJsonArrayString(root.path("suggestedOptionsJson").asText(blankToDefault(request.getOptionsJson(), "[]"))))
                .aiHint("以下内容为 AI 辅助建议，请教师确认后再写回题库。")
                .build();
    }

    private JsonNode parseJsonContent(String content) {
        try {
            String normalized = content.trim();
            if (normalized.startsWith("```")) {
                int firstBrace = normalized.indexOf('{');
                int lastBrace = normalized.lastIndexOf('}');
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    normalized = normalized.substring(firstBrace, lastBrace + 1);
                }
            }
            return objectMapper.readTree(normalized);
        } catch (Exception exception) {
            throw new BusinessException(5002, "AI 返回结果无法解析为 JSON，请稍后重试");
        }
    }

    private String asJsonArrayString(String value) {
        if (!StringUtils.hasText(value)) {
            return "[]";
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private double defaultScore(String questionType) {
        if ("MULTIPLE_CHOICE".equalsIgnoreCase(questionType)) return 10D;
        if ("SHORT_ANSWER".equalsIgnoreCase(questionType)) return 15D;
        return 5D;
    }
}
