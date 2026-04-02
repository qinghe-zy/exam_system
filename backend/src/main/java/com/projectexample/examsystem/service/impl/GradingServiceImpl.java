package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.GradeAnswerItemRequest;
import com.projectexample.examsystem.dto.GradingSubmitRequest;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.GradingRecord;
import com.projectexample.examsystem.entity.InAppMessage;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.AuditLogMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.GradingRecordMapper;
import com.projectexample.examsystem.mapper.InAppMessageMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.GradingService;
import com.projectexample.examsystem.vo.CandidateAnswerItemVO;
import com.projectexample.examsystem.vo.GradingTaskVO;
import com.projectexample.examsystem.vo.GradingWorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final AnswerSheetMapper answerSheetMapper;
    private final AnswerItemMapper answerItemMapper;
    private final ExamPlanMapper examPlanMapper;
    private final QuestionBankMapper questionBankMapper;
    private final GradingRecordMapper gradingRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;
    private final InAppMessageMapper inAppMessageMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<GradingTaskVO> listTasks() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> planIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).in(ExamPlan::getOrganizationId, accessibleOrgIds))
                .stream().map(ExamPlan::getId).toList();

        return answerSheetMapper.selectList(Wrappers.lambdaQuery(AnswerSheet.class)
                        .in(!accessScopeService.isAdmin(), AnswerSheet::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED"))
                        .orderByDesc(AnswerSheet::getSubmittedAt, AnswerSheet::getId))
                .stream()
                .map(sheet -> {
                    ExamPlan plan = examPlanMapper.selectById(sheet.getExamPlanId());
                    List<AnswerItem> items = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                            .eq(AnswerItem::getAnswerSheetId, sheet.getId()));
                    long subjectiveCount = items.stream().filter(item -> !isObjective(item.getQuestionType())).count();
                    long pendingCount = items.stream().filter(item -> !isObjective(item.getQuestionType()) && !"GRADED".equals(item.getStatus())).count();
                    return GradingTaskVO.builder()
                            .answerSheetId(sheet.getId())
                            .examName(plan == null ? sheet.getPaperName() : plan.getExamName())
                            .candidateName(sheet.getCandidateName())
                            .submittedAt(sheet.getSubmittedAt())
                            .objectiveScore(sheet.getObjectiveScore())
                            .subjectiveQuestionCount((int) subjectiveCount)
                            .pendingQuestionCount((int) pendingCount)
                            .status(sheet.getStatus())
                            .build();
                })
                .toList();
    }

    @Override
    public GradingWorkspaceVO getWorkspace(Long answerSheetId) {
        AnswerSheet sheet = requireSheet(answerSheetId);
        ExamPlan plan = examPlanMapper.selectById(sheet.getExamPlanId());
        if (plan != null && !accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(plan.getOrganizationId());
        }
        return buildWorkspace(sheet, plan);
    }

    @Override
    public GradingWorkspaceVO submitGrading(Long answerSheetId, GradingSubmitRequest request, String username) {
        AnswerSheet sheet = requireSheet(answerSheetId);
        ExamPlan plan = examPlanMapper.selectById(sheet.getExamPlanId());
        if (plan != null && !accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(plan.getOrganizationId());
        }
        SysUser grader = requireUser(username);

        Map<Long, GradeAnswerItemRequest> gradingMap = request.getGradeItems().stream()
                .collect(Collectors.toMap(GradeAnswerItemRequest::getAnswerItemId, Function.identity()));
        List<AnswerItem> answerItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                .eq(AnswerItem::getAnswerSheetId, answerSheetId));

        for (AnswerItem item : answerItems) {
            if (isObjective(item.getQuestionType())) {
                continue;
            }
            GradeAnswerItemRequest grade = gradingMap.get(item.getId());
            if (grade == null) {
                continue;
            }
            item.setScoreAwarded(grade.getScoreAwarded());
            item.setMarkedFlag(1);
            item.setReviewComment(grade.getReviewComment());
            item.setStatus("GRADED");
            answerItemMapper.updateById(item);

            GradingRecord record = new GradingRecord();
            record.setAnswerSheetId(answerSheetId);
            record.setAnswerItemId(item.getId());
            record.setGraderId(grader.getId());
            record.setGraderName(grader.getFullName() == null ? grader.getNickname() : grader.getFullName());
            record.setScoreAwarded(grade.getScoreAwarded());
            record.setCommentText(grade.getReviewComment());
            record.setGradedAt(LocalDateTime.now());
            record.setStatus("FINALIZED");
            gradingRecordMapper.insert(record);
        }

        List<AnswerItem> refreshedItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                .eq(AnswerItem::getAnswerSheetId, answerSheetId));
        double subjectiveScore = refreshedItems.stream()
                .filter(item -> !isObjective(item.getQuestionType()))
                .map(AnswerItem::getScoreAwarded)
                .mapToDouble(value -> value == null ? 0D : value)
                .sum();
        boolean pending = refreshedItems.stream()
                .anyMatch(item -> !isObjective(item.getQuestionType()) && !"GRADED".equals(item.getStatus()));

        sheet.setSubjectiveScore(subjectiveScore);
        sheet.setFinalScore((sheet.getObjectiveScore() == null ? 0D : sheet.getObjectiveScore()) + subjectiveScore);
        sheet.setStatus(pending ? "PARTIALLY_GRADED" : "GRADED");
        answerSheetMapper.updateById(sheet);

        ExamRecord record = examRecordMapper.selectOne(Wrappers.lambdaQuery(ExamRecord.class)
                .eq(ExamRecord::getAnswerSheetId, answerSheetId)
                .last("limit 1"));
        if (record != null) {
            record.setSubjectiveScore(subjectiveScore);
            record.setFinalScore(sheet.getFinalScore());
            record.setPassedFlag(plan != null && sheet.getFinalScore() >= plan.getPassScore() ? 1 : 0);
            record.setPublishedFlag(pending ? 0 : 1);
            record.setStatus(pending ? "PARTIALLY_GRADED" : "PUBLISHED");
            examRecordMapper.updateById(record);
            if (!pending) {
                InAppMessage message = new InAppMessage();
                message.setRecipientUserId(record.getUserId());
                message.setTitle("成绩发布提醒");
                message.setMessageType("SCORE_PUBLISH");
                message.setContent("考试《" + record.getExamName() + "》成绩已发布，请及时查看。");
                message.setRelatedType("SCORE_RECORD");
                message.setRelatedId(record.getId());
                message.setReadFlag(0);
                inAppMessageMapper.insert(message);
            }
        }

        AuditLog log = new AuditLog();
        log.setOperatorId(grader.getId());
        log.setOperatorName(grader.getFullName() == null ? grader.getNickname() : grader.getFullName());
        log.setModuleName("GRADING");
        log.setActionName("SUBMIT_GRADE");
        log.setTargetType("ANSWER_SHEET");
        log.setTargetId(answerSheetId);
        log.setDetailText("Updated grading for answer sheet " + answerSheetId);
        auditLogMapper.insert(log);

        return buildWorkspace(answerSheetMapper.selectById(answerSheetId), plan);
    }

    private GradingWorkspaceVO buildWorkspace(AnswerSheet sheet, ExamPlan plan) {
        List<AnswerItem> items = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                .eq(AnswerItem::getAnswerSheetId, sheet.getId())
                .orderByAsc(AnswerItem::getQuestionOrder, AnswerItem::getId));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(items.stream().map(AnswerItem::getQuestionId).toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));

        List<CandidateAnswerItemVO> answerItemVOs = items.stream()
                .map(item -> {
                    QuestionBank question = questionMap.get(item.getQuestionId());
                    return CandidateAnswerItemVO.builder()
                            .answerItemId(item.getId())
                            .questionId(item.getQuestionId())
                            .questionOrder(item.getQuestionOrder())
                            .questionCode(question == null ? null : question.getQuestionCode())
                            .questionType(item.getQuestionType())
                            .stem(question == null ? null : question.getStem())
                            .optionsJson(question == null ? null : question.getOptionsJson())
                            .maxScore(item.getMaxScore())
                            .answerContent(item.getAnswerContent())
                            .scoreAwarded(item.getScoreAwarded())
                            .status(item.getStatus())
                            .markedFlag(item.getMarkedFlag())
                            .reviewComment(item.getReviewComment())
                            .build();
                })
                .toList();

        return GradingWorkspaceVO.builder()
                .answerSheetId(sheet.getId())
                .examName(plan == null ? sheet.getPaperName() : plan.getExamName())
                .candidateName(sheet.getCandidateName())
                .objectiveScore(sheet.getObjectiveScore())
                .subjectiveScore(sheet.getSubjectiveScore())
                .finalScore(sheet.getFinalScore())
                .items(answerItemVOs)
                .build();
    }

    private AnswerSheet requireSheet(Long answerSheetId) {
        AnswerSheet sheet = answerSheetMapper.selectById(answerSheetId);
        if (sheet == null) {
            throw new BusinessException(4040, "Answer sheet not found");
        }
        return sheet;
    }

    private SysUser requireUser(String username) {
        SysUser user = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, username).last("limit 1"));
        if (user == null) {
            throw new BusinessException(4010, "User session is invalid");
        }
        return user;
    }

    private boolean isObjective(String questionType) {
        return List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "JUDGE").contains(questionType);
    }
}
