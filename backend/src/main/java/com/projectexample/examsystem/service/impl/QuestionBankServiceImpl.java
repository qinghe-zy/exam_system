package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.KnowledgePointAutoGroupRequest;
import com.projectexample.examsystem.dto.KnowledgePointQuotaItemRequest;
import com.projectexample.examsystem.dto.QuestionImportRequest;
import com.projectexample.examsystem.dto.QuestionBankSaveRequest;
import com.projectexample.examsystem.entity.PaperQuestion;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.PaperQuestionMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.security.ExamPeriodProtectionService;
import com.projectexample.examsystem.service.QuestionBankService;
import com.projectexample.examsystem.vo.QuestionBankVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankMapper questionBankMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final AccessScopeService accessScopeService;
    private final ExamPeriodProtectionService examPeriodProtectionService;

    @Override
    public List<QuestionBankVO> listQuestions() {
        List<QuestionBank> entities = loadAccessibleQuestions();
        return toVOListWithUsage(entities);
    }

    @Override
    public List<QuestionBankVO> exportQuestions() {
        return listQuestions();
    }

    @Override
    public List<QuestionBankVO> importQuestions(QuestionImportRequest request) {
        return request.getQuestions().stream()
                .map(this::createQuestion)
                .toList();
    }

    @Override
    public QuestionBankVO createQuestion(QuestionBankSaveRequest request) {
        QuestionBank entity = new QuestionBank();
        apply(entity, request);
        questionBankMapper.insert(entity);
        return toVO(requireEntity(entity.getId()), 0L);
    }

    @Override
    public QuestionBankVO updateQuestion(Long id, QuestionBankSaveRequest request) {
        examPeriodProtectionService.assertMutable("更新题目");
        QuestionBank entity = requireEntity(id);
        apply(entity, request);
        questionBankMapper.updateById(entity);
        return toVO(requireEntity(id), countUsage(id));
    }

    @Override
    public void deleteQuestion(Long id) {
        examPeriodProtectionService.assertMutable("删除题目");
        requireEntity(id);
        questionBankMapper.deleteById(id);
    }

    @Override
    public List<QuestionBankVO> autoGroupByKnowledgePoint(KnowledgePointAutoGroupRequest request) {
        List<QuestionBank> accessibleQuestions = loadAccessibleQuestions().stream()
                .filter(item -> request.getSubject().equals(item.getSubject()))
                .filter(item -> !StringUtils.hasText(request.getDifficultyLevel()) || request.getDifficultyLevel().equals(item.getDifficultyLevel()))
                .filter(item -> !StringUtils.hasText(request.getQuestionType()) || request.getQuestionType().equals(item.getQuestionType()))
                .toList();
        List<QuestionBank> selected = new ArrayList<>();
        for (KnowledgePointQuotaItemRequest quota : request.getQuotas()) {
            if (quota.getQuestionCount() == null || quota.getQuestionCount() <= 0) {
                continue;
            }
            List<QuestionBank> pool = accessibleQuestions.stream()
                    .filter(item -> quota.getKnowledgePoint().equals(item.getKnowledgePoint()))
                    .filter(item -> selected.stream().noneMatch(existing -> existing.getId().equals(item.getId())))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (pool.size() < quota.getQuestionCount()) {
                throw new BusinessException(4004, "知识点“" + quota.getKnowledgePoint() + "”可用题目不足，无法生成题组");
            }
            Collections.shuffle(pool, ThreadLocalRandom.current());
            selected.addAll(pool.subList(0, quota.getQuestionCount()));
        }
        return toVOListWithUsage(selected);
    }

    private List<QuestionBank> loadAccessibleQuestions() {
        List<Long> accessibleIds = accessScopeService.accessibleOrganizationIds();
        return questionBankMapper.selectList(Wrappers.lambdaQuery(QuestionBank.class)
                .in(!accessScopeService.isAdmin(), QuestionBank::getOrganizationId, accessibleIds.isEmpty() ? List.of(-1L) : accessibleIds)
                .orderByDesc(QuestionBank::getUpdateTime, QuestionBank::getId));
    }

    private QuestionBank requireEntity(Long id) {
        QuestionBank entity = questionBankMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(4040, "Question record not found");
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(entity.getOrganizationId());
        }
        return entity;
    }

    private void apply(QuestionBank entity, QuestionBankSaveRequest request) {
        entity.setOrganizationId(accessScopeService.currentUser().getOrganizationId());
        entity.setQuestionCode(request.getQuestionCode());
        entity.setSubject(request.getSubject());
        entity.setQuestionType(request.getQuestionType());
        entity.setDifficultyLevel(request.getDifficultyLevel());
        entity.setStem(request.getStem());
        entity.setStemHtml(request.getStemHtml());
        entity.setMaterialContent(request.getMaterialContent());
        entity.setAttachmentJson(request.getAttachmentJson());
        entity.setOptionsJson(request.getOptionsJson());
        entity.setAnswerKey(request.getAnswerKey());
        entity.setAnalysisText(request.getAnalysisText());
        entity.setKnowledgePoint(request.getKnowledgePoint());
        entity.setChapterName(request.getChapterName());
        entity.setSourceName(request.getSourceName());
        entity.setTags(request.getTags());
        entity.setDefaultScore(request.getDefaultScore());
        entity.setReviewerStatus(request.getReviewerStatus());
        entity.setVersionNo(request.getVersionNo());
        entity.setStatus(request.getStatus());
    }

    private List<QuestionBankVO> toVOListWithUsage(List<QuestionBank> entities) {
        Map<Long, Long> usageMap = usageMap(entities.stream().map(QuestionBank::getId).toList());
        return entities.stream()
                .map(entity -> toVO(entity, usageMap.getOrDefault(entity.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> usageMap(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                        .in(PaperQuestion::getQuestionId, questionIds))
                .stream()
                .collect(Collectors.groupingBy(PaperQuestion::getQuestionId, Collectors.counting()));
    }

    private long countUsage(Long questionId) {
        return paperQuestionMapper.selectCount(Wrappers.lambdaQuery(PaperQuestion.class)
                .eq(PaperQuestion::getQuestionId, questionId));
    }

    private QuestionBankVO toVO(QuestionBank entity, Long usageCount) {
        return QuestionBankVO.builder()
                .id(entity.getId())
                .questionCode(entity.getQuestionCode())
                .subject(entity.getSubject())
                .questionType(entity.getQuestionType())
                .difficultyLevel(entity.getDifficultyLevel())
                .stem(entity.getStem())
                .stemHtml(entity.getStemHtml())
                .materialContent(entity.getMaterialContent())
                .attachmentJson(entity.getAttachmentJson())
                .optionsJson(entity.getOptionsJson())
                .answerKey(entity.getAnswerKey())
                .analysisText(entity.getAnalysisText())
                .knowledgePoint(entity.getKnowledgePoint())
                .chapterName(entity.getChapterName())
                .sourceName(entity.getSourceName())
                .tags(entity.getTags())
                .defaultScore(entity.getDefaultScore())
                .reviewerStatus(entity.getReviewerStatus())
                .versionNo(entity.getVersionNo())
                .status(entity.getStatus())
                .usageCount(usageCount)
                .build();
    }
}
