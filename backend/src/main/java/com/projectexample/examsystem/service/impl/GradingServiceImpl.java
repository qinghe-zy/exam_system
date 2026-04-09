package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.GradeAnswerItemRequest;
import com.projectexample.examsystem.dto.GradingReviewRequest;
import com.projectexample.examsystem.dto.GradingSubmitRequest;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.GradingRecord;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.entity.ScoreAppeal;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.AuditLogMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.GradingRecordMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.ScoreAppealMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.GradingService;
import com.projectexample.examsystem.service.NotificationService;
import com.projectexample.examsystem.vo.CandidateAnswerItemVO;
import com.projectexample.examsystem.vo.GradingTaskVO;
import com.projectexample.examsystem.vo.GradingWorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final ScoreAppealMapper scoreAppealMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;
    private final AccessScopeService accessScopeService;
    private final NotificationService notificationService;

    @Override
    public List<GradingTaskVO> listTasks() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> planIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                        .in(ExamPlan::getOrganizationId, accessibleOrgIds)).stream().map(ExamPlan::getId).toList();

        List<Long> sheetIds = answerSheetMapper.selectList(Wrappers.lambdaQuery(AnswerSheet.class)
                        .in(!accessScopeService.isAdmin(), AnswerSheet::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED", "REVIEW_PENDING", "REJUDGING"))
                        .orderByDesc(AnswerSheet::getSubmittedAt, AnswerSheet::getId))
                .stream()
                .map(AnswerSheet::getId)
                .toList();

        Map<Long, ExamRecord> recordMap = examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class)
                        .in(ExamRecord::getAnswerSheetId, sheetIds.isEmpty() ? List.of(-1L) : sheetIds))
                .stream()
                .collect(Collectors.toMap(ExamRecord::getAnswerSheetId, Function.identity(), (left, right) -> right));

        return answerSheetMapper.selectList(Wrappers.lambdaQuery(AnswerSheet.class)
                        .in(!accessScopeService.isAdmin(), AnswerSheet::getExamPlanId, planIds.isEmpty() ? List.of(-1L) : planIds)
                        .in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED", "REVIEW_PENDING", "REJUDGING"))
                        .orderByDesc(AnswerSheet::getSubmittedAt, AnswerSheet::getId))
                .stream()
                .map(sheet -> {
                    ExamPlan plan = examPlanMapper.selectById(sheet.getExamPlanId());
                    List<AnswerItem> items = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                            .eq(AnswerItem::getAnswerSheetId, sheet.getId()));
                    long subjectiveCount = items.stream().filter(item -> !isObjective(item.getQuestionType())).count();
                    long pendingCount = items.stream()
                            .filter(item -> !isObjective(item.getQuestionType()) && !"GRADED".equals(item.getStatus()))
                            .count();
                    ExamRecord record = recordMap.get(sheet.getId());
                    return GradingTaskVO.builder()
                            .answerSheetId(sheet.getId())
                            .examName(plan == null ? sheet.getPaperName() : plan.getExamName())
                            .candidateName(sheet.getCandidateName())
                            .submittedAt(sheet.getSubmittedAt())
                            .objectiveScore(sheet.getObjectiveScore())
                            .subjectiveQuestionCount((int) subjectiveCount)
                            .pendingQuestionCount((int) pendingCount)
                            .status(sheet.getStatus())
                            .reviewStatus(record == null ? null : record.getReviewStatus())
                            .appealStatus(record == null ? null : record.getAppealStatus())
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
        return buildWorkspace(sheet, plan, findRecord(answerSheetId));
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
            record.setGraderName(resolveUserName(grader));
            record.setReviewRound(resolveNextRound(item.getId()));
            record.setGradingAction("REJUDGING".equalsIgnoreCase(sheet.getStatus()) ? "REJUDGE" : "INITIAL");
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
        sheet.setStatus(pending ? "PARTIALLY_GRADED" : "REVIEW_PENDING");
        answerSheetMapper.updateById(sheet);

        ExamRecord record = findRecord(answerSheetId);
        if (record != null) {
            record.setSubjectiveScore(subjectiveScore);
            record.setFinalScore(sheet.getFinalScore());
            record.setPassedFlag(plan != null && sheet.getFinalScore() >= plan.getPassScore() ? 1 : 0);
            record.setPublishedFlag(0);
            record.setReviewStatus(pending ? "IN_PROGRESS" : "PENDING");
            if (!StringUtils.hasText(record.getAppealStatus())) {
                record.setAppealStatus("NONE");
            }
            record.setStatus(pending ? "PARTIALLY_GRADED" : "REVIEW_PENDING");
            examRecordMapper.updateById(record);
        }

        writeAuditLog(grader, "GRADING", "SUBMIT_GRADE", "ANSWER_SHEET", answerSheetId,
                "完成阅卷评分提交：" + (plan == null ? answerSheetId : plan.getExamName()));
        return buildWorkspace(answerSheetMapper.selectById(answerSheetId), plan, findRecord(answerSheetId));
    }

    @Override
    public GradingWorkspaceVO reviewGrading(Long answerSheetId, GradingReviewRequest request, String username) {
        AnswerSheet sheet = requireSheet(answerSheetId);
        ExamPlan plan = examPlanMapper.selectById(sheet.getExamPlanId());
        if (plan != null && !accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(plan.getOrganizationId());
        }
        ExamRecord record = requireRecord(answerSheetId);
        SysUser reviewer = requireUser(username);
        String action = normalizeReviewAction(request.getAction());

        if (!List.of("REVIEW_PENDING", "REJUDGING").contains(sheet.getStatus())) {
            throw new BusinessException(4005, "当前答卷状态不支持复核处理");
        }

        if ("APPROVE".equals(action)) {
            sheet.setStatus("PUBLISHED");
            record.setStatus("PUBLISHED");
            record.setReviewStatus("APPROVED");
            record.setPublishedFlag(1);
            if ("APPROVED_REJUDGE".equals(record.getAppealStatus())) {
                record.setAppealStatus("RESOLVED");
                markLatestAppealResolved(record.getId(), reviewer, request.getReviewComment());
            } else if (!StringUtils.hasText(record.getAppealStatus())) {
                record.setAppealStatus("NONE");
            }
            notificationService.sendScorePublishedNotification(record);
        } else {
            sheet.setStatus("REJUDGING");
            record.setStatus("REJUDGING");
            record.setReviewStatus("REJUDGE_REQUIRED");
            record.setPublishedFlag(0);
        }

        answerSheetMapper.updateById(sheet);
        examRecordMapper.updateById(record);
        writeAuditLog(reviewer, "GRADING", action, "ANSWER_SHEET", answerSheetId,
                "复核处理答卷：" + (plan == null ? answerSheetId : plan.getExamName()) + "，备注：" + defaultText(request.getReviewComment()));
        return buildWorkspace(answerSheetMapper.selectById(answerSheetId), plan, findRecord(answerSheetId));
    }

    private void markLatestAppealResolved(Long scoreRecordId, SysUser reviewer, String reviewComment) {
        ScoreAppeal appeal = scoreAppealMapper.selectOne(Wrappers.lambdaQuery(ScoreAppeal.class)
                .eq(ScoreAppeal::getScoreRecordId, scoreRecordId)
                .eq(ScoreAppeal::getStatus, "APPROVED_REJUDGE")
                .orderByDesc(ScoreAppeal::getProcessedAt, ScoreAppeal::getId)
                .last("limit 1"));
        if (appeal == null) {
            return;
        }
        appeal.setStatus("RESOLVED");
        appeal.setProcessedBy(reviewer.getId());
        appeal.setProcessedByName(resolveUserName(reviewer));
        appeal.setProcessedAt(LocalDateTime.now());
        if (StringUtils.hasText(reviewComment)) {
            appeal.setProcessComment(trimValue(reviewComment, 1000));
        }
        scoreAppealMapper.updateById(appeal);
        ExamRecord record = examRecordMapper.selectById(scoreRecordId);
        if (record != null) {
            notificationService.sendScoreAppealResultNotification(
                    record,
                    "申诉重判完成",
                    "你的申诉重判已经完成，《" + record.getExamName() + "》的最新成绩已重新发布。"
            );
        }
    }

    private GradingWorkspaceVO buildWorkspace(AnswerSheet sheet, ExamPlan plan, ExamRecord record) {
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
                            .stemHtml(question == null ? null : question.getStemHtml())
                            .materialContent(question == null ? null : question.getMaterialContent())
                            .attachmentJson(question == null ? null : question.getAttachmentJson())
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
                .status(sheet.getStatus())
                .reviewStatus(record == null ? null : record.getReviewStatus())
                .appealStatus(record == null ? null : record.getAppealStatus())
                .items(answerItemVOs)
                .build();
    }

    private ExamRecord findRecord(Long answerSheetId) {
        return examRecordMapper.selectOne(Wrappers.lambdaQuery(ExamRecord.class)
                .eq(ExamRecord::getAnswerSheetId, answerSheetId)
                .last("limit 1"));
    }

    private ExamRecord requireRecord(Long answerSheetId) {
        ExamRecord record = findRecord(answerSheetId);
        if (record == null) {
            throw new BusinessException(4040, "成绩记录不存在");
        }
        return record;
    }

    private AnswerSheet requireSheet(Long answerSheetId) {
        AnswerSheet sheet = answerSheetMapper.selectById(answerSheetId);
        if (sheet == null) {
            throw new BusinessException(4040, "Answer sheet not found");
        }
        return sheet;
    }

    private SysUser requireUser(String username) {
        SysUser user = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(4010, "User session is invalid");
        }
        return user;
    }

    private int resolveNextRound(Long answerItemId) {
        Integer latestRound = gradingRecordMapper.selectList(Wrappers.lambdaQuery(GradingRecord.class)
                        .eq(GradingRecord::getAnswerItemId, answerItemId)
                        .orderByDesc(GradingRecord::getReviewRound, GradingRecord::getId))
                .stream()
                .map(GradingRecord::getReviewRound)
                .filter(value -> value != null)
                .findFirst()
                .orElse(0);
        return latestRound + 1;
    }

    private String normalizeReviewAction(String action) {
        String normalized = String.valueOf(action).trim().toUpperCase();
        if (!List.of("APPROVE", "REJECT_REJUDGE").contains(normalized)) {
            throw new BusinessException(4004, "不支持的复核动作");
        }
        return normalized;
    }

    private void writeAuditLog(SysUser user, String module, String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperatorId(user.getId());
        log.setOperatorName(resolveUserName(user));
        log.setModuleName(module);
        log.setActionName(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetailText(detail);
        auditLogMapper.insert(log);
    }

    private String resolveUserName(SysUser user) {
        return user.getFullName() == null ? user.getNickname() : user.getFullName();
    }

    private String trimValue(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "未填写";
    }

    private boolean isObjective(String questionType) {
        return List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "JUDGE", "FILL_BLANK").contains(questionType);
    }
}
