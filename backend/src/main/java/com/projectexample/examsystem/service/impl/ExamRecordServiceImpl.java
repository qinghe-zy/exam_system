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
import com.projectexample.examsystem.vo.CandidateWrongQuestionVO;
import com.projectexample.examsystem.vo.ExamRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    public String exportRecordsCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("考生,考试,试卷,提交时间,客观分,主观分,总分,是否及格,发布状态,复核状态,申诉状态,成绩状态\n");
        listRecords().forEach(record -> builder.append(csv(record.getCandidateName())).append(',')
                .append(csv(record.getExamName())).append(',')
                .append(csv(record.getPaperName())).append(',')
                .append(csv(String.valueOf(record.getSubmittedAt()))).append(',')
                .append(csv(String.valueOf(record.getObjectiveScore()))).append(',')
                .append(csv(String.valueOf(record.getSubjectiveScore()))).append(',')
                .append(csv(String.valueOf(record.getFinalScore()))).append(',')
                .append(csv(record.getPassedFlag() != null && record.getPassedFlag() == 1 ? "及格" : "不及格")).append(',')
                .append(csv(record.getPublishedFlag() != null && record.getPublishedFlag() == 1 ? "已发布" : "未发布")).append(',')
                .append(csv(record.getReviewStatus())).append(',')
                .append(csv(record.getAppealStatus())).append(',')
                .append(csv(record.getStatus())).append('\n'));
        return builder.toString();
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
        return buildDetail(requirePublishedRecord(recordId, user.getId()));
    }

    @Override
    public List<CandidateWrongQuestionVO> listMyWrongQuestions(String username) {
        SysUser user = requireUser(username);
        List<ExamRecord> records = examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                        .eq(ExamRecord::getUserId, user.getId())
                        .eq(ExamRecord::getPublishedFlag, 1)
                        .orderByDesc(ExamRecord::getSubmittedAt, ExamRecord::getId));
        if (records.isEmpty()) {
            return List.of();
        }

        Map<Long, ExamRecord> recordMap = records.stream()
                .collect(Collectors.toMap(ExamRecord::getAnswerSheetId, Function.identity()));
        List<AnswerItem> wrongItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                        .in(AnswerItem::getAnswerSheetId, recordMap.keySet())
                        .orderByDesc(AnswerItem::getUpdateTime, AnswerItem::getId))
                .stream()
                .filter(this::isWrongAnswer)
                .toList();
        if (wrongItems.isEmpty()) {
            return List.of();
        }

        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(wrongItems.stream()
                        .map(AnswerItem::getQuestionId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));

        return wrongItems.stream()
                .collect(Collectors.groupingBy(AnswerItem::getQuestionId))
                .entrySet()
                .stream()
                .map(entry -> toWrongQuestionVO(entry.getValue(), questionMap.get(entry.getKey()), recordMap))
                .sorted(Comparator
                        .comparing(CandidateWrongQuestionVO::getLatestSubmittedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CandidateWrongQuestionVO::getMistakeCount,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CandidateWrongQuestionVO::getQuestionId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
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
                .reviewStatus(record.getReviewStatus())
                .appealStatus(record.getAppealStatus())
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

    private ExamRecord requirePublishedRecord(Long recordId, Long userId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(4040, "成绩记录不存在");
        }
        if (record.getPublishedFlag() == null || record.getPublishedFlag() != 1) {
            throw new BusinessException(4005, "当前成绩尚未发布，暂不可查看详情");
        }
        return record;
    }

    private CandidateScoreDetailVO buildDetail(ExamRecord record) {
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
                            .stemHtml(question == null ? null : question.getStemHtml())
                            .materialContent(question == null ? null : question.getMaterialContent())
                            .attachmentJson(question == null ? null : question.getAttachmentJson())
                            .optionsJson(question == null ? null : question.getOptionsJson())
                            .knowledgePoint(question == null ? null : question.getKnowledgePoint())
                            .chapterName(question == null ? null : question.getChapterName())
                            .answerContent(answerItem.getAnswerContent())
                            .referenceAnswer(question == null ? null : question.getAnswerKey())
                            .analysisText(question == null ? null : question.getAnalysisText())
                            .maxScore(answerItem.getMaxScore())
                            .scoreAwarded(answerItem.getScoreAwarded())
                            .status(answerItem.getStatus())
                            .reviewLaterFlag(answerItem.getReviewLaterFlag())
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
                .reviewStatus(record.getReviewStatus())
                .appealStatus(record.getAppealStatus())
                .status(record.getStatus())
                .items(items)
                .build();
    }

    private CandidateWrongQuestionVO toWrongQuestionVO(List<AnswerItem> wrongItems,
                                                      QuestionBank question,
                                                      Map<Long, ExamRecord> recordMap) {
        AnswerItem latestItem = wrongItems.stream()
                .max(Comparator.comparing(item -> resolveSubmittedAt(recordMap.get(item.getAnswerSheetId())),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(wrongItems.get(0));
        ExamRecord latestRecord = recordMap.get(latestItem.getAnswerSheetId());
        return CandidateWrongQuestionVO.builder()
                .questionId(latestItem.getQuestionId())
                .questionCode(question == null ? null : question.getQuestionCode())
                .questionType(latestItem.getQuestionType())
                .stem(question == null ? null : question.getStem())
                .stemHtml(question == null ? null : question.getStemHtml())
                .materialContent(question == null ? null : question.getMaterialContent())
                .attachmentJson(question == null ? null : question.getAttachmentJson())
                .optionsJson(question == null ? null : question.getOptionsJson())
                .knowledgePoint(question == null ? null : question.getKnowledgePoint())
                .chapterName(question == null ? null : question.getChapterName())
                .referenceAnswer(question == null ? null : question.getAnswerKey())
                .analysisText(question == null ? null : question.getAnalysisText())
                .latestRecordId(latestRecord == null ? null : latestRecord.getId())
                .latestExamName(latestRecord == null ? null : latestRecord.getExamName())
                .latestPaperName(latestRecord == null ? null : latestRecord.getPaperName())
                .latestSubmittedAt(resolveSubmittedAt(latestRecord))
                .latestAnswerContent(latestItem.getAnswerContent())
                .latestMaxScore(latestItem.getMaxScore())
                .latestScoreAwarded(latestItem.getScoreAwarded())
                .latestReviewLaterFlag(latestItem.getReviewLaterFlag())
                .mistakeCount(wrongItems.size())
                .build();
    }

    private LocalDateTime resolveSubmittedAt(ExamRecord record) {
        return record == null ? null : record.getSubmittedAt();
    }

    private boolean isWrongAnswer(AnswerItem answerItem) {
        double maxScore = answerItem.getMaxScore() == null ? 0D : answerItem.getMaxScore();
        double scoreAwarded = answerItem.getScoreAwarded() == null ? 0D : answerItem.getScoreAwarded();
        return scoreAwarded < maxScore;
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
