package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.ExamPaperSaveRequest;
import com.projectexample.examsystem.dto.PaperQuestionItemRequest;
import com.projectexample.examsystem.entity.ExamPaper;
import com.projectexample.examsystem.entity.PaperQuestion;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.ExamPaperMapper;
import com.projectexample.examsystem.mapper.PaperQuestionMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.ExamPaperService;
import com.projectexample.examsystem.vo.ExamPaperVO;
import com.projectexample.examsystem.vo.PaperQuestionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperMapper examPaperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AccessScopeService accessScopeService;

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
        applyPaper(entity, request);
        examPaperMapper.updateById(entity);
        replacePaperQuestions(id, request.getQuestionItems());
        return toVO(requireEntity(id));
    }

    @Override
    public void deletePaper(Long id) {
        requireEntity(id);
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
        entity.setOrganizationId(accessScopeService.currentUser().getOrganizationId());
        entity.setPaperCode(request.getPaperCode());
        entity.setPaperName(request.getPaperName());
        entity.setSubject(request.getSubject());
        entity.setAssemblyMode(request.getAssemblyMode());
        entity.setDescriptionText(request.getDescriptionText());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setTotalScore(request.getTotalScore());
        entity.setPassScore(request.getPassScore());
        entity.setQuestionCount(request.getQuestionItems().size());
        entity.setPublishStatus(request.getPublishStatus());
    }

    private void replacePaperQuestions(Long paperId, List<PaperQuestionItemRequest> questionItems) {
        paperQuestionMapper.delete(Wrappers.lambdaQuery(PaperQuestion.class).eq(PaperQuestion::getPaperId, paperId));
        for (PaperQuestionItemRequest item : questionItems) {
            if (questionBankMapper.selectById(item.getQuestionId()) == null) {
                throw new BusinessException(4041, "Question " + item.getQuestionId() + " does not exist");
            }
            requireQuestionAccessible(item.getQuestionId());
            PaperQuestion entity = new PaperQuestion();
            entity.setPaperId(paperId);
            entity.setQuestionId(item.getQuestionId());
            entity.setSortNo(item.getSortNo());
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
                .durationMinutes(entity.getDurationMinutes())
                .totalScore(entity.getTotalScore())
                .passScore(entity.getPassScore())
                .questionCount(entity.getQuestionCount())
                .publishStatus(entity.getPublishStatus())
                .questionItems(items)
                .build();
    }

    private void requireQuestionAccessible(Long questionId) {
        QuestionBank question = questionBankMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(4041, "Question " + questionId + " does not exist");
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(question.getOrganizationId());
        }
    }
}
