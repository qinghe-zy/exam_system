package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectexample.examsystem.common.PaperRuleConfigItem;
import com.projectexample.examsystem.dto.ExamPaperSaveRequest;
import com.projectexample.examsystem.dto.PaperQuestionItemRequest;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamPaper;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.PaperQuestion;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.ExamPaperMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.PaperQuestionMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.ExamPaperService;
import com.projectexample.examsystem.vo.ExamPaperVO;
import com.projectexample.examsystem.vo.PaperQuestionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperMapper examPaperMapper;
    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AccessScopeService accessScopeService;
    private final ObjectMapper objectMapper;

    @Override
    public List<ExamPaperVO> listPapers() {
        List<Long> accessibleIds = accessScopeService.accessibleOrganizationIds();
        return examPaperMapper.selectList(Wrappers.lambdaQuery(ExamPaper.class)
                        .in(!accessScopeService.isAdmin(), ExamPaper::getOrganizationId, accessibleIds)
                        .orderByDesc(ExamPaper::getUpdateTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ExamPaperVO getPaper(Long id) {
        return toVO(requireEntity(id));
    }

    @Override
    public ExamPaperVO createPaper(ExamPaperSaveRequest request) {
        ExamPaper entity = new ExamPaper();
        applyPaper(entity, request);
        examPaperMapper.insert(entity);
        replacePaperQuestions(entity.getId(), request.getQuestionItems());
        return toVO(requireEntity(entity.getId()));
    }

    @Override
    public ExamPaperVO updatePaper(Long id, ExamPaperSaveRequest request) {
        ExamPaper entity = requireEntity(id);
        assertPaperMutable(entity, "更新");
        applyPaper(entity, request);
        examPaperMapper.updateById(entity);
        replacePaperQuestions(id, request.getQuestionItems());
        return toVO(requireEntity(id));
    }

    @Override
    public void deletePaper(Long id) {
        ExamPaper entity = requireEntity(id);
        assertPaperMutable(entity, "删除");
        paperQuestionMapper.delete(Wrappers.lambdaQuery(PaperQuestion.class).eq(PaperQuestion::getPaperId, id));
        examPaperMapper.deleteById(id);
    }

    private ExamPaper requireEntity(Long id) {
        ExamPaper entity = examPaperMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(4040, "Exam paper not found");
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(entity.getOrganizationId());
        }
        return entity;
    }

    private void applyPaper(ExamPaper entity, ExamPaperSaveRequest request) {
        Map<Long, QuestionBank> selectedQuestionMap = loadSelectedQuestions(request.getQuestionItems());
        double computedTotalScore = request.getQuestionItems().stream()
                .map(PaperQuestionItemRequest::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        if (request.getPassScore() > computedTotalScore) {
            throw new BusinessException(4004, "及格线不能高于试卷总分");
        }

        List<PaperRuleConfigItem> typeConfigs = request.getQuestionTypeConfigs();
        List<PaperRuleConfigItem> difficultyConfigs = request.getDifficultyConfigs();
        if (typeConfigs == null || typeConfigs.isEmpty()) {
            typeConfigs = deriveQuestionTypeConfigs(request.getQuestionItems(), selectedQuestionMap);
        }
        if (difficultyConfigs == null || difficultyConfigs.isEmpty()) {
            difficultyConfigs = deriveDifficultyConfigs(request.getQuestionItems(), selectedQuestionMap);
        }

        entity.setOrganizationId(resolvePaperOrganization(selectedQuestionMap.values()));
        entity.setPaperCode(request.getPaperCode());
        entity.setPaperName(request.getPaperName());
        entity.setSubject(request.getSubject());
        entity.setAssemblyMode(request.getAssemblyMode());
        entity.setDescriptionText(request.getDescriptionText());
        entity.setPaperVersion(request.getPaperVersion());
        entity.setRemarkText(request.getRemarkText());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setTotalScore(computedTotalScore);
        entity.setPassScore(request.getPassScore());
        entity.setQuestionCount(request.getQuestionItems().size());
        entity.setShuffleEnabled(request.getShuffleEnabled());
        entity.setQuestionTypeConfigJson(writeRuleConfigs(typeConfigs));
        entity.setDifficultyConfigJson(writeRuleConfigs(difficultyConfigs));
        entity.setPublishStatus(request.getPublishStatus());
    }

    private void replacePaperQuestions(Long paperId, List<PaperQuestionItemRequest> questionItems) {
        paperQuestionMapper.delete(Wrappers.lambdaQuery(PaperQuestion.class).eq(PaperQuestion::getPaperId, paperId));
        List<PaperQuestionItemRequest> normalizedItems = questionItems.stream()
                .sorted(Comparator.comparing(PaperQuestionItemRequest::getSortNo).thenComparing(PaperQuestionItemRequest::getQuestionId))
                .toList();
        Map<Long, Long> duplicateCheck = normalizedItems.stream()
                .collect(Collectors.groupingBy(PaperQuestionItemRequest::getQuestionId, LinkedHashMap::new, Collectors.counting()));
        List<Long> duplicateQuestionIds = duplicateCheck.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicateQuestionIds.isEmpty()) {
            throw new BusinessException(4004, "同一题目不能重复加入试卷：" + duplicateQuestionIds);
        }

        int sortNo = 1;
        for (PaperQuestionItemRequest item : normalizedItems) {
            if (questionBankMapper.selectById(item.getQuestionId()) == null) {
                throw new BusinessException(4041, "题目不存在：" + item.getQuestionId());
            }
            requireQuestionAccessible(item.getQuestionId());
            PaperQuestion entity = new PaperQuestion();
            entity.setPaperId(paperId);
            entity.setQuestionId(item.getQuestionId());
            entity.setSortNo(sortNo++);
            entity.setScore(item.getScore());
            entity.setRequiredFlag(item.getRequiredFlag());
            paperQuestionMapper.insert(entity);
        }
    }

    private ExamPaperVO toVO(ExamPaper entity) {
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                .eq(PaperQuestion::getPaperId, entity.getId())
                .orderByAsc(PaperQuestion::getSortNo, PaperQuestion::getId));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(
                        paperQuestions.stream().map(PaperQuestion::getQuestionId).toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));

        List<PaperQuestionItemVO> items = paperQuestions.stream()
                .sorted(Comparator.comparing(PaperQuestion::getSortNo))
                .map(item -> {
                    QuestionBank question = questionMap.get(item.getQuestionId());
                    return PaperQuestionItemVO.builder()
                            .questionId(item.getQuestionId())
                            .sortNo(item.getSortNo())
                            .score(item.getScore())
                            .requiredFlag(item.getRequiredFlag())
                            .questionCode(question == null ? null : question.getQuestionCode())
                            .questionType(question == null ? null : question.getQuestionType())
                            .difficultyLevel(question == null ? null : question.getDifficultyLevel())
                            .stem(question == null ? null : question.getStem())
                            .build();
                })
                .toList();

        return ExamPaperVO.builder()
                .id(entity.getId())
                .paperCode(entity.getPaperCode())
                .paperName(entity.getPaperName())
                .subject(entity.getSubject())
                .assemblyMode(entity.getAssemblyMode())
                .descriptionText(entity.getDescriptionText())
                .paperVersion(entity.getPaperVersion())
                .remarkText(entity.getRemarkText())
                .durationMinutes(entity.getDurationMinutes())
                .totalScore(entity.getTotalScore())
                .passScore(entity.getPassScore())
                .questionCount(entity.getQuestionCount())
                .shuffleEnabled(entity.getShuffleEnabled())
                .questionTypeConfigs(resolveQuestionTypeConfigs(entity, items, questionMap))
                .difficultyConfigs(resolveDifficultyConfigs(entity, items, questionMap))
                .publishStatus(entity.getPublishStatus())
                .questionItems(items)
                .build();
    }

    private void requireQuestionAccessible(Long questionId) {
        QuestionBank question = questionBankMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(4041, "题目不存在：" + questionId);
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(question.getOrganizationId());
        }
    }

    private Map<Long, QuestionBank> loadSelectedQuestions(List<PaperQuestionItemRequest> questionItems) {
        List<Long> questionIds = questionItems.stream().map(PaperQuestionItemRequest::getQuestionId).distinct().toList();
        Map<Long, QuestionBank> selectedQuestionMap = questionBankMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        if (selectedQuestionMap.size() != questionIds.size()) {
            throw new BusinessException(4041, "存在未找到的题目，无法保存试卷");
        }
        selectedQuestionMap.values().forEach(question -> requireQuestionAccessible(question.getId()));
        return selectedQuestionMap;
    }

    private Long resolvePaperOrganization(Iterable<QuestionBank> questions) {
        Long currentOrgId = accessScopeService.currentUser().getOrganizationId();
        Long firstOrgId = null;
        for (QuestionBank question : questions) {
            if (question.getOrganizationId() == null) {
                continue;
            }
            if (firstOrgId == null) {
                firstOrgId = question.getOrganizationId();
            } else if (!Objects.equals(firstOrgId, question.getOrganizationId())) {
                throw new BusinessException(4004, "当前试卷不能混用不同组织下的题目");
            }
        }
        return firstOrgId == null ? currentOrgId : firstOrgId;
    }

    private List<PaperRuleConfigItem> deriveQuestionTypeConfigs(List<PaperQuestionItemRequest> questionItems,
                                                                Map<Long, QuestionBank> questionMap) {
        Map<String, PaperRuleConfigItem> result = new LinkedHashMap<>();
        for (PaperQuestionItemRequest item : questionItems) {
            QuestionBank question = questionMap.get(item.getQuestionId());
            if (question == null) {
                continue;
            }
            String key = question.getQuestionType();
            PaperRuleConfigItem current = result.getOrDefault(key, PaperRuleConfigItem.builder()
                    .code(key)
                    .label(labelQuestionType(key))
                    .count(0)
                    .score(item.getScore())
                    .build());
            current.setCount(current.getCount() + 1);
            if (current.getScore() == null) {
                current.setScore(item.getScore());
            }
            result.put(key, current);
        }
        return new ArrayList<>(result.values());
    }

    private List<PaperRuleConfigItem> deriveDifficultyConfigs(List<PaperQuestionItemRequest> questionItems,
                                                              Map<Long, QuestionBank> questionMap) {
        Map<String, PaperRuleConfigItem> result = new LinkedHashMap<>();
        for (PaperQuestionItemRequest item : questionItems) {
            QuestionBank question = questionMap.get(item.getQuestionId());
            if (question == null) {
                continue;
            }
            String key = question.getDifficultyLevel();
            PaperRuleConfigItem current = result.getOrDefault(key, PaperRuleConfigItem.builder()
                    .code(key)
                    .label(labelDifficulty(key))
                    .count(0)
                    .build());
            current.setCount(current.getCount() + 1);
            result.put(key, current);
        }
        return new ArrayList<>(result.values());
    }

    private List<PaperRuleConfigItem> resolveQuestionTypeConfigs(ExamPaper entity,
                                                                 List<PaperQuestionItemVO> items,
                                                                 Map<Long, QuestionBank> questionMap) {
        List<PaperRuleConfigItem> configs = readRuleConfigs(entity.getQuestionTypeConfigJson());
        if (!configs.isEmpty()) {
            return configs;
        }
        List<PaperQuestionItemRequest> requestItems = items.stream()
                .map(item -> {
                    PaperQuestionItemRequest request = new PaperQuestionItemRequest();
                    request.setQuestionId(item.getQuestionId());
                    request.setSortNo(item.getSortNo());
                    request.setScore(item.getScore());
                    request.setRequiredFlag(item.getRequiredFlag());
                    return request;
                })
                .toList();
        return deriveQuestionTypeConfigs(requestItems, questionMap);
    }

    private List<PaperRuleConfigItem> resolveDifficultyConfigs(ExamPaper entity,
                                                               List<PaperQuestionItemVO> items,
                                                               Map<Long, QuestionBank> questionMap) {
        List<PaperRuleConfigItem> configs = readRuleConfigs(entity.getDifficultyConfigJson());
        if (!configs.isEmpty()) {
            return configs;
        }
        List<PaperQuestionItemRequest> requestItems = items.stream()
                .map(item -> {
                    PaperQuestionItemRequest request = new PaperQuestionItemRequest();
                    request.setQuestionId(item.getQuestionId());
                    request.setSortNo(item.getSortNo());
                    request.setScore(item.getScore());
                    request.setRequiredFlag(item.getRequiredFlag());
                    return request;
                })
                .toList();
        return deriveDifficultyConfigs(requestItems, questionMap);
    }

    private String writeRuleConfigs(List<PaperRuleConfigItem> configs) {
        try {
            return objectMapper.writeValueAsString(configs == null ? List.of() : configs);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(5000, "试卷规则配置序列化失败");
        }
    }

    private List<PaperRuleConfigItem> readRuleConfigs(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<List<PaperRuleConfigItem>>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException(5000, "试卷规则配置解析失败");
        }
    }

    private String labelQuestionType(String value) {
        return switch (String.valueOf(value).toUpperCase(Locale.ROOT)) {
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTIPLE_CHOICE" -> "多选题";
            case "TRUE_FALSE", "JUDGE" -> "判断题";
            case "SHORT_ANSWER" -> "简答题";
            default -> value;
        };
    }

    private String labelDifficulty(String value) {
        return switch (String.valueOf(value).toUpperCase(Locale.ROOT)) {
            case "EASY" -> "简单";
            case "MEDIUM" -> "中等";
            case "HARD" -> "困难";
            default -> value;
        };
    }

    private void assertPaperMutable(ExamPaper paper, String actionName) {
        List<ExamPlan> relatedPlans = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                .eq(ExamPlan::getPaperId, paper.getId()));
        boolean started = relatedPlans.stream().anyMatch(this::isPlanStarted);
        if (started) {
            throw new BusinessException(4005, "当前试卷已被进行中的考试使用，暂不允许" + actionName + "试卷");
        }
        List<Long> planIds = relatedPlans.stream().map(ExamPlan::getId).toList();
        if (!planIds.isEmpty()) {
            long answerSheetCount = answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class)
                    .in(AnswerSheet::getExamPlanId, planIds));
            if (answerSheetCount > 0) {
                throw new BusinessException(4005, "当前试卷已产生答卷，暂不允许" + actionName + "试卷");
            }
        }
    }

    private boolean isPlanStarted(ExamPlan plan) {
        return plan.getPublishStatus() != null
                && plan.getPublishStatus() == 1
                && plan.getStartTime() != null
                && !LocalDateTime.now().isBefore(plan.getStartTime());
    }
}
