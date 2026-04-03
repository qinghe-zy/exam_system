package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.ExamRecordService;
import com.projectexample.examsystem.vo.CandidateScoreDetailVO;
import com.projectexample.examsystem.vo.CandidateScoreItemVO;
import com.projectexample.examsystem.vo.ExamRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamRecordServiceImpl implements ExamRecordService {

    private final ExamRecordMapper examRecordMapper;
    private final ExamPlanMapper examPlanMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AnswerItemMapper answerItemMapper;
    private final SysUserMapper sysUserMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<ExamRecordVO> listRecords() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> planIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).in(ExamPlan::getOrganizationId, accessibleOrgIds))
                .stream().map(ExamPlan::getId).toList();

        return examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                        .in(!accessScopeService.isAdmin(), ExamRecord::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .orderByDesc(ExamRecord::getSubmittedAt, ExamRecord::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<ExamRecordVO> listMyRecords(String username) {
        SysUser user = requireUser(username);
        return examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                        .eq(ExamRecord::getUserId, user.getId())
                        .eq(ExamRecord::getPublishedFlag, 1)
                        .orderByDesc(ExamRecord::getSubmittedAt, ExamRecord::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CandidateScoreDetailVO getMyRecordDetail(Long recordId, String username) {
        SysUser user = requireUser(username);
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !user.getId().equals(record.getUserId())) {
            throw new BusinessException(4040, "成绩记录不存在");
        }
        if (record.getPublishedFlag() == null || record.getPublishedFlag() != 1) {
            throw new BusinessException(4005, "当前成绩尚未发布，暂不可查看详情");
        }

        List<AnswerItem> answerItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                .eq(AnswerItem::getAnswerSheetId, record.getAnswerSheetId())
                .orderByAsc(AnswerItem::getQuestionOrder, AnswerItem::getId));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(answerItems.stream()
                        .map(AnswerItem::getQuestionId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));

        List<CandidateScoreItemVO> items = answerItems.stream()
                .map(answerItem -> {
                    QuestionBank question = questionMap.get(answerItem.getQuestionId());
                    return CandidateScoreItemVO.builder()
                            .questionId(answerItem.getQuestionId())
                            .questionOrder(answerItem.getQuestionOrder())
                            .questionCode(question == null ? null : question.getQuestionCode())
                            .questionType(answerItem.getQuestionType())
                            .stem(question == null ? null : question.getStem())
                            .optionsJson(question == null ? null : question.getOptionsJson())
                            .answerContent(answerItem.getAnswerContent())
                            .referenceAnswer(question == null ? null : question.getAnswerKey())
                            .analysisText(question == null ? null : question.getAnalysisText())
                            .maxScore(answerItem.getMaxScore())
                            .scoreAwarded(answerItem.getScoreAwarded())
                            .status(answerItem.getStatus())
                            .reviewComment(answerItem.getReviewComment())
                            .build();
                })
                .toList();

        return CandidateScoreDetailVO.builder()
                .id(record.getId())
                .examPlanId(record.getExamPlanId())
                .answerSheetId(record.getAnswerSheetId())
                .examName(record.getExamName())
                .paperName(record.getPaperName())
                .candidateName(record.getCandidateName())
                .submittedAt(record.getSubmittedAt())
                .objectiveScore(record.getObjectiveScore())
                .subjectiveScore(record.getSubjectiveScore())
                .finalScore(record.getFinalScore())
                .passedFlag(record.getPassedFlag())
                .publishedFlag(record.getPublishedFlag())
                .status(record.getStatus())
                .items(items)
                .build();
    }

    private ExamRecordVO toVO(ExamRecord record) {
        return ExamRecordVO.builder()
                .id(record.getId())
                .examPlanId(record.getExamPlanId())
                .answerSheetId(record.getAnswerSheetId())
                .candidateName(record.getCandidateName())
                .examName(record.getExamName())
                .paperName(record.getPaperName())
                .submittedAt(record.getSubmittedAt())
                .objectiveScore(record.getObjectiveScore())
                .subjectiveScore(record.getSubjectiveScore())
                .finalScore(record.getFinalScore())
                .passedFlag(record.getPassedFlag())
                .publishedFlag(record.getPublishedFlag())
                .status(record.getStatus())
                .build();
    }

    private SysUser requireUser(String username) {
        SysUser user = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(4010, "登录状态已失效，请重新登录");
        }
        return user;
    }
}
