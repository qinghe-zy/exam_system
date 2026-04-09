package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.ExamPlanSaveRequest;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamCandidate;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamPaper;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.ExamCandidateMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamPaperMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.ExamPlanService;
import com.projectexample.examsystem.service.NotificationService;
import com.projectexample.examsystem.vo.ExamPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ExamPlanServiceImpl implements ExamPlanService {

    private final ExamPlanMapper examPlanMapper;
    private final ExamPaperMapper examPaperMapper;
    private final SysUserMapper sysUserMapper;
    private final ExamCandidateMapper examCandidateMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final AccessScopeService accessScopeService;
    private final NotificationService notificationService;

    @Override
    public List<ExamPlanVO> listPlans() {
        List<Long> accessibleIds = accessScopeService.accessibleOrganizationIds();
        return examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                        .in(!accessScopeService.isAdmin(), ExamPlan::getOrganizationId, accessibleIds)
                        .orderByDesc(ExamPlan::getStartTime, ExamPlan::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ExamPlanVO createPlan(ExamPlanSaveRequest request) {
        ExamPaper paper = requirePaper(request.getPaperId());
        ExamPlan entity = new ExamPlan();
        apply(entity, request, paper);
        examPlanMapper.insert(entity);
        replaceCandidates(entity.getId(), request.getCandidateUserIds());
        publishMessagesIfNeeded(entity.getId(), entity.getExamName(), request.getPublishStatus());
        return toVO(requirePlan(entity.getId()));
    }

    @Override
    public ExamPlanVO updatePlan(Long id, ExamPlanSaveRequest request) {
        ExamPlan entity = requirePlan(id);
        assertPlanMutable(entity, "更新");
        if (request.getSourceExamPlanId() != null && request.getSourceExamPlanId().equals(id)) {
            throw new BusinessException(4004, "原考试不能关联当前正在编辑的考试计划");
        }
        ExamPaper paper = requirePaper(request.getPaperId());
        apply(entity, request, paper);
        examPlanMapper.updateById(entity);
        replaceCandidates(id, request.getCandidateUserIds());
        publishMessagesIfNeeded(id, entity.getExamName(), request.getPublishStatus());
        return toVO(requirePlan(id));
    }

    @Override
    public void deletePlan(Long id) {
        ExamPlan plan = requirePlan(id);
        assertPlanMutable(plan, "删除");
        examCandidateMapper.delete(Wrappers.lambdaQuery(ExamCandidate.class).eq(ExamCandidate::getExamPlanId, id));
        examPlanMapper.deleteById(id);
    }

    @Override
    public String exportSignInSheetCsv(Long id) {
        ExamPlan plan = requirePlan(id);
        List<ExamCandidate> candidates = examCandidateMapper.selectList(Wrappers.lambdaQuery(ExamCandidate.class)
                .eq(ExamCandidate::getExamPlanId, id)
                .orderByAsc(ExamCandidate::getSeatNo, ExamCandidate::getId));
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("考试编码,考试名称,考试类型,批次,考场,座位号,考生姓名,组织,签到状态,签到时间,准入码\n");
        candidates.forEach(candidate -> builder.append(csv(plan.getExamCode())).append(',')
                .append(csv(plan.getExamName())).append(',')
                .append(csv(plan.getExamMode())).append(',')
                .append(csv(plan.getBatchLabel())).append(',')
                .append(csv(plan.getExamRoom())).append(',')
                .append(csv(candidate.getSeatNo())).append(',')
                .append(csv(candidate.getCandidateName())).append(',')
                .append(csv(candidate.getOrganizationName())).append(',')
                .append(csv(defaultFlag(candidate.getSignedInFlag()) == 1 ? "已签到" : "未签到")).append(',')
                .append(csv(candidate.getSignedInAt() == null ? "" : candidate.getSignedInAt().toString())).append(',')
                .append(csv(candidate.getAccessCode())).append('\n'));
        return builder.toString();
    }

    private ExamPlan requirePlan(Long id) {
        ExamPlan plan = examPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(4040, "Exam plan not found");
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(plan.getOrganizationId());
        }
        return plan;
    }

    private ExamPaper requirePaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(4041, "试卷不存在");
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(paper.getOrganizationId());
        }
        return paper;
    }

    private void apply(ExamPlan entity, ExamPlanSaveRequest request, ExamPaper paper) {
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BusinessException(4004, "考试结束时间必须晚于开始时间");
        }
        if (request.getPassScore() > paper.getTotalScore()) {
            throw new BusinessException(4004, "考试及格线不能高于试卷总分");
        }
        String examMode = normalizeExamMode(request.getExamMode());
        ExamPlan sourcePlan = resolveSourcePlan(request.getSourceExamPlanId(), paper.getOrganizationId());
        if (!"NORMAL".equals(examMode) && sourcePlan == null) {
            throw new BusinessException(4004, "补考、缓考或重考必须关联原考试");
        }
        entity.setOrganizationId(paper.getOrganizationId() == null ? accessScopeService.currentUser().getOrganizationId() : paper.getOrganizationId());
        entity.setExamCode(request.getExamCode());
        entity.setExamName(request.getExamName());
        entity.setExamMode(examMode);
        entity.setBatchLabel(trimToLength(request.getBatchLabel(), 128));
        entity.setExamRoom(trimToLength(request.getExamRoom(), 128));
        entity.setSourceExamPlanId(sourcePlan == null ? null : sourcePlan.getId());
        entity.setSourceExamName(sourcePlan == null ? null : trimToLength(sourcePlan.getExamName(), 128));
        entity.setPaperId(paper.getId());
        entity.setPaperName(paper.getPaperName());
        entity.setSubject(paper.getSubject());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setPassScore(request.getPassScore());
        entity.setCandidateScope(request.getCandidateScope());
        entity.setAttemptLimit(request.getAttemptLimit());
        entity.setExamPassword(request.getExamPassword());
        entity.setLateEntryMinutes(request.getLateEntryMinutes() == null ? 15 : request.getLateEntryMinutes());
        entity.setSignInRequired(request.getSignInRequired() == null ? 0 : request.getSignInRequired());
        entity.setSignInStartMinutes(request.getSignInStartMinutes() == null ? 60 : Math.max(request.getSignInStartMinutes(), 0));
        entity.setEarlySubmitMinutes(request.getEarlySubmitMinutes() == null ? 0 : request.getEarlySubmitMinutes());
        entity.setAutoSubmitEnabled(request.getAutoSubmitEnabled() == null ? 1 : request.getAutoSubmitEnabled());
        entity.setAntiCheatLevel(request.getAntiCheatLevel() == null ? "BASIC" : request.getAntiCheatLevel());
        entity.setInstructionText(request.getInstructionText());
        entity.setStatus(request.getStatus());
        entity.setPublishStatus(request.getPublishStatus());
    }

    private void replaceCandidates(Long examPlanId, List<Long> candidateUserIds) {
        examCandidateMapper.delete(Wrappers.lambdaQuery(ExamCandidate.class).eq(ExamCandidate::getExamPlanId, examPlanId));
        AtomicInteger seatIndex = new AtomicInteger(1);
        for (Long userId : candidateUserIds) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(4042, "Candidate user not found: " + userId);
            }
            if (!accessScopeService.isAdmin()) {
                accessScopeService.assertOrganizationAccessible(user.getOrganizationId());
            }
            ExamCandidate candidate = new ExamCandidate();
            candidate.setExamPlanId(examPlanId);
            candidate.setUserId(userId);
            candidate.setCandidateName(user.getFullName() == null ? user.getNickname() : user.getFullName());
            candidate.setOrganizationName(user.getOrganizationName());
            candidate.setStatus("ASSIGNED");
            candidate.setAttemptCount(0);
            candidate.setSignedInFlag(0);
            candidate.setSignedInAt(null);
            candidate.setAccessCode("EX" + examPlanId + "-" + userId);
            candidate.setSeatNo(String.format(Locale.ROOT, "A%02d", seatIndex.getAndIncrement()));
            examCandidateMapper.insert(candidate);
        }
    }

    private ExamPlanVO toVO(ExamPlan entity) {
        List<ExamCandidate> candidates = examCandidateMapper.selectList(Wrappers.lambdaQuery(ExamCandidate.class)
                .eq(ExamCandidate::getExamPlanId, entity.getId()));
        long signedInCount = candidates.stream().filter(item -> defaultFlag(item.getSignedInFlag()) == 1).count();
        long submittedCount = answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class)
                .eq(AnswerSheet::getExamPlanId, entity.getId())
                .in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED", "GRADED")));
        return ExamPlanVO.builder()
                .id(entity.getId())
                .examCode(entity.getExamCode())
                .examName(entity.getExamName())
                .examMode(entity.getExamMode())
                .batchLabel(entity.getBatchLabel())
                .examRoom(entity.getExamRoom())
                .sourceExamPlanId(entity.getSourceExamPlanId())
                .sourceExamName(entity.getSourceExamName())
                .paperId(entity.getPaperId())
                .paperName(entity.getPaperName())
                .subject(entity.getSubject())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .durationMinutes(entity.getDurationMinutes())
                .passScore(entity.getPassScore())
                .candidateScope(entity.getCandidateScope())
                .attemptLimit(entity.getAttemptLimit())
                .examPassword(entity.getExamPassword())
                .lateEntryMinutes(entity.getLateEntryMinutes())
                .signInRequired(entity.getSignInRequired())
                .signInStartMinutes(entity.getSignInStartMinutes())
                .earlySubmitMinutes(entity.getEarlySubmitMinutes())
                .autoSubmitEnabled(entity.getAutoSubmitEnabled())
                .antiCheatLevel(entity.getAntiCheatLevel())
                .instructionText(entity.getInstructionText())
                .status(entity.getStatus())
                .publishStatus(entity.getPublishStatus())
                .candidateCount(candidates.size())
                .signedInCount((int) signedInCount)
                .signInRate(candidates.isEmpty() ? 0D : Math.round((signedInCount * 10000D / candidates.size())) / 100D)
                .submittedCount((int) submittedCount)
                .candidateUserIds(candidates.stream().map(ExamCandidate::getUserId).toList())
                .build();
    }

    private void publishMessagesIfNeeded(Long examPlanId, String examName, Integer publishStatus) {
        if (publishStatus == null || publishStatus != 1) {
            return;
        }
        List<ExamCandidate> candidates = examCandidateMapper.selectList(Wrappers.lambdaQuery(ExamCandidate.class)
                .eq(ExamCandidate::getExamPlanId, examPlanId));
        ExamPlan plan = examPlanMapper.selectById(examPlanId);
        notificationService.sendExamPublishNotifications(
                plan == null ? null : plan.getOrganizationId(),
                examPlanId,
                examName,
                plan == null ? null : plan.getStartTime(),
                candidates
        );
    }

    private void assertPlanMutable(ExamPlan plan, String actionName) {
        long answerSheetCount = answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class)
                .eq(AnswerSheet::getExamPlanId, plan.getId()));
        boolean started = plan.getPublishStatus() != null
                && plan.getPublishStatus() == 1
                && plan.getStartTime() != null
                && !LocalDateTime.now().isBefore(plan.getStartTime());
        if (started || answerSheetCount > 0) {
            throw new BusinessException(4005, "当前考试已开始或已有答卷，暂不允许" + actionName + "考试计划");
        }
    }

    private String normalizeExamMode(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "NORMAL";
        if (!List.of("NORMAL", "MAKEUP", "DEFERRED", "RETAKE").contains(normalized)) {
            throw new BusinessException(4004, "不支持的考试类型");
        }
        return normalized;
    }

    private ExamPlan resolveSourcePlan(Long sourceExamPlanId, Long organizationId) {
        if (sourceExamPlanId == null) {
            return null;
        }
        ExamPlan plan = examPlanMapper.selectById(sourceExamPlanId);
        if (plan == null) {
            throw new BusinessException(4040, "关联原考试不存在");
        }
        if (!accessScopeService.isAdmin()) {
            accessScopeService.assertOrganizationAccessible(plan.getOrganizationId());
        }
        if (organizationId != null && plan.getOrganizationId() != null && !organizationId.equals(plan.getOrganizationId())) {
            throw new BusinessException(4004, "关联原考试必须与当前试卷处于同一组织范围");
        }
        return plan;
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private Integer defaultFlag(Integer value) {
        return value == null ? 0 : value;
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
