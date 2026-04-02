package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.QuestionImportRequest;
import com.projectexample.examsystem.dto.QuestionBankSaveRequest;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.service.QuestionBankService;
import com.projectexample.examsystem.vo.QuestionBankVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankMapper questionBankMapper;

    @Override
    public List<QuestionBankVO> listQuestions() {
        return questionBankMapper.selectList(Wrappers.lambdaQuery(QuestionBank.class).orderByDesc(QuestionBank::getUpdateTime))
                .stream()
                .map(this::toVO)
                .toList();
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
        return toVO(requireEntity(entity.getId()));
    }

    @Override
    public QuestionBankVO updateQuestion(Long id, QuestionBankSaveRequest request) {
        QuestionBank entity = requireEntity(id);
        apply(entity, request);
        questionBankMapper.updateById(entity);
        return toVO(requireEntity(id));
    }

    @Override
    public void deleteQuestion(Long id) {
        requireEntity(id);
        questionBankMapper.deleteById(id);
    }

    private QuestionBank requireEntity(Long id) {
        QuestionBank entity = questionBankMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(4040, "Question record not found");
        }
        return entity;
    }

    private void apply(QuestionBank entity, QuestionBankSaveRequest request) {
        entity.setQuestionCode(request.getQuestionCode());
        entity.setSubject(request.getSubject());
        entity.setQuestionType(request.getQuestionType());
        entity.setDifficultyLevel(request.getDifficultyLevel());
        entity.setStem(request.getStem());
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

    private QuestionBankVO toVO(QuestionBank entity) {
        return QuestionBankVO.builder()
                .id(entity.getId())
                .questionCode(entity.getQuestionCode())
                .subject(entity.getSubject())
                .questionType(entity.getQuestionType())
                .difficultyLevel(entity.getDifficultyLevel())
                .stem(entity.getStem())
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
                .build();
    }
}
