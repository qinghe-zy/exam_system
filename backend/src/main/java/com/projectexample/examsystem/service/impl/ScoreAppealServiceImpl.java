package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.ScoreAppealProcessRequest;
import com.projectexample.examsystem.dto.ScoreAppealSubmitRequest;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.ScoreAppeal;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.AuditLogMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.ScoreAppealMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.NotificationService;
import com.projectexample.examsystem.service.ScoreAppealService;
import com.projectexample.examsystem.vo.ScoreAppealVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreAppealServiceImpl implements ScoreAppealService {

    private final ScoreAppealMapper scoreAppealMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;
    private final AccessScopeService accessScopeService;
    private final NotificationService notificationService;

    @Override
    public List<ScoreAppealVO> listAppeals(Long scoreRecordId) {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> accessiblePlanIds = accessScopeService.isAdmin()
                ? examPlanMapper.selectList(null).stream().map(ExamPlan::getId).toList()
                : examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                        .in(ExamPlan::getOrganizationId, accessibleOrgIds)).stream().map(ExamPlan::getId).toList();

        return scoreAppealMapper.selectList(Wrappers.lambdaQuery(ScoreAppeal.class)
                        .eq(scoreRecordId != null, ScoreAppeal::getScoreRecordId, scoreRecordId)
                        .in(!accessScopeService.isAdmin(), ScoreAppeal::getExamPlanId, accessiblePlanIds.isEmpty() ? List.of(-1L) : accessiblePlanIds)
                        .orderByDesc(ScoreAppeal::getSubmittedAt, ScoreAppeal::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<ScoreAppealVO> listMyAppeals(Long scoreRecordId, String username) {
        SysUser user = requireUser(username);
        ExamRecord record = requireOwnPublishedRecord(scoreRecordId, user.getId());
        return scoreAppealMapper.selectList(Wrappers.lambdaQuery(ScoreAppeal.class)
                        .eq(ScoreAppeal::getScoreRecordId, record.getId())
                        .eq(ScoreAppeal::getUserId, user.getId())
                        .orderByDesc(ScoreAppeal::getSubmittedAt, ScoreAppeal::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ScoreAppealVO submitAppeal(Long scoreRecordId, ScoreAppealSubmitRequest request, String username) {
        SysUser user = requireUser(username);
        ExamRecord record = requireOwnPublishedRecord(scoreRecordId, user.getId());
        ensureNoActiveAppeal(record.getId());

        ScoreAppeal appeal = new ScoreAppeal();
        appeal.setScoreRecordId(record.getId());
        appeal.setAnswerSheetId(record.getAnswerSheetId());
        appeal.setExamPlanId(record.getExamPlanId());
        appeal.setUserId(user.getId());
        appeal.setCandidateName(record.getCandidateName());
        appeal.setExamName(record.getExamName());
        appeal.setAppealReason(request.getAppealReason());
        appeal.setExpectedOutcome(trimValue(request.getExpectedOutcome(), 255));
        appeal.setStatus("SUBMITTED");
        appeal.setSubmittedAt(LocalDateTime.now());
        scoreAppealMapper.insert(appeal);

        record.setAppealStatus("SUBMITTED");
        examRecordMapper.updateById(record);

        List<SysUser> managers = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
                .in(SysUser::getRoleCode, List.of("ADMIN", "ORG_ADMIN", "GRADER"))
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        notificationService.sendScoreAppealSubmittedNotification(record, appeal, managers);
        writeAuditLog(user, "SCORE_APPEAL", "SUBMIT_APPEAL", "SCORE_RECORD", record.getId(),
                "考生提交成绩申诉：" + record.getExamName());
        return toVO(scoreAppealMapper.selectById(appeal.getId()));
    }

    @Override
    public ScoreAppealVO processAppeal(Long appealId, ScoreAppealProcessRequest request, String username) {
        SysUser operator = requireUser(username);
        ScoreAppeal appeal = requireAppeal(appealId);
        ExamPlan plan = examPlanMapper.selectById(appeal.getExamPlanId());
        if (plan != null && !accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(plan.getOrganizationId());
        }
        if (!"SUBMITTED".equalsIgnoreCase(appeal.getStatus())) {
            throw new BusinessException(4005, "当前申诉已处理，不能重复操作");
        }

        String action = normalizeAction(request.getAction());
        ExamRecord record = requireRecord(appeal.getScoreRecordId());
        AnswerSheet sheet = requireSheet(appeal.getAnswerSheetId());

        appeal.setResolutionAction(action);
        appeal.setProcessComment(trimValue(request.getProcessComment(), 1000));
        appeal.setProcessedBy(operator.getId());
        appeal.setProcessedByName(resolveOperatorName(operator));
        appeal.setProcessedAt(LocalDateTime.now());

        if ("REJECT".equals(action)) {
            appeal.setStatus("REJECTED");
            record.setAppealStatus("REJECTED");
            notificationService.sendScoreAppealResultNotification(
                    record,
                    "成绩申诉处理结果",
                    "你的成绩申诉未通过，处理意见：" + defaultText(appeal.getProcessComment())
            );
        } else {
            appeal.setStatus("APPROVED_REJUDGE");
            record.setAppealStatus("APPROVED_REJUDGE");
            record.setReviewStatus("REJUDGE_REQUIRED");
            record.setPublishedFlag(0);
            record.setStatus("REJUDGING");
            sheet.setStatus("REJUDGING");
            answerSheetMapper.updateById(sheet);
            notificationService.sendScoreAppealResultNotification(
                    record,
                    "成绩申诉处理结果",
                    "你的成绩申诉已通过，系统已进入重判流程。"
            );
        }

        scoreAppealMapper.updateById(appeal);
        examRecordMapper.updateById(record);
        writeAuditLog(operator, "SCORE_APPEAL", action, "SCORE_APPEAL", appeal.getId(),
                "处理成绩申诉：" + appeal.getExamName());
        return toVO(scoreAppealMapper.selectById(appealId));
    }

    private void ensureNoActiveAppeal(Long scoreRecordId) {
        Long activeCount = scoreAppealMapper.selectCount(Wrappers.lambdaQuery(ScoreAppeal.class)
                .eq(ScoreAppeal::getScoreRecordId, scoreRecordId)
                .in(ScoreAppeal::getStatus, List.of("SUBMITTED", "APPROVED_REJUDGE")));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(4005, "当前成绩已有处理中申诉，请等待处理完成后再提交");
        }
    }

    private ScoreAppeal requireAppeal(Long appealId) {
        ScoreAppeal appeal = scoreAppealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(4040, "成绩申诉不存在");
        }
        return appeal;
    }

    private ExamRecord requireOwnPublishedRecord(Long recordId, Long userId) {
        ExamRecord record = requireRecord(recordId);
        if (!userId.equals(record.getUserId())) {
            throw new BusinessException(4040, "成绩记录不存在");
        }
        if (record.getPublishedFlag() == null || record.getPublishedFlag() != 1) {
            throw new BusinessException(4005, "当前成绩尚未发布，暂不支持申诉");
        }
        return record;
    }

    private ExamRecord requireRecord(Long id) {
        ExamRecord record = examRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(4040, "成绩记录不存在");
        }
        return record;
    }

    private AnswerSheet requireSheet(Long id) {
        AnswerSheet sheet = answerSheetMapper.selectById(id);
        if (sheet == null) {
            throw new BusinessException(4040, "答卷不存在");
        }
        return sheet;
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

    private String normalizeAction(String action) {
        String normalized = String.valueOf(action).trim().toUpperCase();
        if (!List.of("REJECT", "REJUDGE").contains(normalized)) {
            throw new BusinessException(4004, "不支持的申诉处理动作");
        }
        return normalized;
    }

    private void writeAuditLog(SysUser operator, String module, String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperatorId(operator.getId());
        log.setOperatorName(resolveOperatorName(operator));
        log.setModuleName(module);
        log.setActionName(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetailText(detail);
        auditLogMapper.insert(log);
    }

    private String resolveOperatorName(SysUser user) {
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

    private ScoreAppealVO toVO(ScoreAppeal entity) {
        return ScoreAppealVO.builder()
                .id(entity.getId())
                .scoreRecordId(entity.getScoreRecordId())
                .answerSheetId(entity.getAnswerSheetId())
                .examPlanId(entity.getExamPlanId())
                .userId(entity.getUserId())
                .candidateName(entity.getCandidateName())
                .examName(entity.getExamName())
                .appealReason(entity.getAppealReason())
                .expectedOutcome(entity.getExpectedOutcome())
                .status(entity.getStatus())
                .resolutionAction(entity.getResolutionAction())
                .processComment(entity.getProcessComment())
                .processedBy(entity.getProcessedBy())
                .processedByName(entity.getProcessedByName())
                .submittedAt(entity.getSubmittedAt())
                .processedAt(entity.getProcessedAt())
                .build();
    }
}
