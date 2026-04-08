package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectexample.examsystem.dto.CandidateAnswerItemRequest;
import com.projectexample.examsystem.dto.CandidateAnswerSheetSaveRequest;
import com.projectexample.examsystem.dto.CandidateEventReportRequest;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.AntiCheatEvent;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.entity.ConfigItem;
import com.projectexample.examsystem.entity.ExamCandidate;
import com.projectexample.examsystem.entity.ExamPaper;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.PaperQuestion;
import com.projectexample.examsystem.entity.QuestionBank;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerItemMapper;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.AntiCheatEventMapper;
import com.projectexample.examsystem.mapper.AuditLogMapper;
import com.projectexample.examsystem.mapper.ConfigItemMapper;
import com.projectexample.examsystem.mapper.ExamCandidateMapper;
import com.projectexample.examsystem.mapper.ExamPaperMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.PaperQuestionMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.service.CandidateExamService;
import com.projectexample.examsystem.vo.CandidateAnswerItemVO;
import com.projectexample.examsystem.vo.CandidateExamVO;
import com.projectexample.examsystem.vo.CandidateExamWorkspaceVO;
import com.projectexample.examsystem.vo.AntiCheatPolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateExamServiceImpl implements CandidateExamService {

    private final SysUserMapper sysUserMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamCandidateMapper examCandidateMapper;
    private final ExamPaperMapper examPaperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final AnswerItemMapper answerItemMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AuditLogMapper auditLogMapper;
    private final AntiCheatEventMapper antiCheatEventMapper;
    private final ConfigItemMapper configItemMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<CandidateExamVO> listMyExams(String username) {
        SysUser user = requireUser(username);
        Map<Long, AnswerSheet> sheetMap = answerSheetMapper.selectList(Wrappers.lambdaQuery(AnswerSheet.class)
                        .eq(AnswerSheet::getUserId, user.getId()))
                .stream()
                .collect(Collectors.toMap(AnswerSheet::getExamPlanId, Function.identity(), (left, right) -> right));

        return examCandidateMapper.selectList(Wrappers.lambdaQuery(ExamCandidate.class)
                        .eq(ExamCandidate::getUserId, user.getId())
                        .orderByDesc(ExamCandidate::getAssignedTime, ExamCandidate::getId))
                .stream()
                .map(candidate -> {
                    ExamPlan plan = requirePlan(candidate.getExamPlanId());
                    AnswerSheet sheet = sheetMap.get(plan.getId());
                    LocalDateTime entryDeadlineAt = computeEntryDeadline(plan);
                    LocalDateTime answerDeadlineAt = sheet == null ? null : computeAnswerDeadline(plan, sheet);
                    return CandidateExamVO.builder()
                            .examPlanId(plan.getId())
                            .examName(plan.getExamName())
                            .paperName(plan.getPaperName())
                            .subject(plan.getSubject())
                            .startTime(plan.getStartTime())
                            .endTime(plan.getEndTime())
                            .entryDeadlineAt(entryDeadlineAt)
                            .answerDeadlineAt(answerDeadlineAt)
                            .durationMinutes(plan.getDurationMinutes())
                            .candidateStatus(candidate.getStatus())
                            .attemptCount(candidate.getAttemptCount())
                            .answerSheetStatus(sheet == null ? "NOT_STARTED" : sheet.getStatus())
                            .build();
                })
                .toList();
    }

    @Override
    public CandidateExamWorkspaceVO getWorkspace(Long examPlanId,
                                                 String examPassword,
                                                 String username,
                                                 String clientIp,
                                                 String deviceFingerprint,
                                                 String deviceInfo) {
        SysUser user = requireUser(username);
        ExamPlan plan = requirePlan(examPlanId);
        ExamCandidate candidate = requireCandidate(plan.getId(), user.getId());
        AnswerSheet sheet = getOrCreateSheet(plan, user, false);
        ensurePlanOpen(plan, sheet);
        ensureAttemptAllowed(plan, candidate, sheet);
        ensurePasswordAllowed(plan, sheet, examPassword);
        ensureSingleDeviceAllowed(plan, user, sheet, deviceFingerprint);
        if (sheet.getStartedAt() == null) {
            sheet = markSheetStarted(sheet);
        }
        sheet = autoSubmitIfNeeded(plan, user, candidate, sheet);
        return buildWorkspace(plan, sheet);
    }

    @Override
    public CandidateExamWorkspaceVO saveAnswers(Long examPlanId, CandidateAnswerSheetSaveRequest request, boolean submit, String username) {
        SysUser user = requireUser(username);
        ExamPlan plan = requirePlan(examPlanId);
        ExamCandidate candidate = requireCandidate(plan.getId(), user.getId());
        AnswerSheet sheet = getOrCreateSheet(plan, user, true);
        ensurePlanOpen(plan, sheet);
        ensureAttemptAllowed(plan, candidate, sheet);

        Map<Long, PaperQuestion> paperQuestionMap = loadPaperQuestionMap(plan.getPaperId());
        Map<Long, QuestionBank> questionMap = loadQuestionMap(paperQuestionMap.keySet());
        Map<Long, AnswerItem> existingItems = loadExistingAnswerItems(sheet.getId());

        upsertAnswerItems(sheet, request.getAnswers(), paperQuestionMap, questionMap, existingItems);
        sheet.setSaveVersion((sheet.getSaveVersion() == null ? 0 : sheet.getSaveVersion()) + 1);
        sheet.setStatus(submit ? "SUBMITTED" : "IN_PROGRESS");

        LocalDateTime answerDeadlineAt = computeAnswerDeadline(plan, sheet);
        if (!submit && LocalDateTime.now().isAfter(answerDeadlineAt)) {
            if (isAutoSubmitEnabled(plan)) {
                finalizeSubmission(plan, user, candidate, sheet, paperQuestionMap, questionMap, existingItems, true);
                return buildWorkspace(plan, answerSheetMapper.selectById(sheet.getId()));
            }
            answerSheetMapper.updateById(sheet);
            throw new BusinessException(4005, "本场考试作答时间已结束，请立即交卷");
        }

        if (submit) {
            ensureEarlySubmitAllowed(plan, sheet);
            finalizeSubmission(plan, user, candidate, sheet, paperQuestionMap, questionMap, existingItems, false);
        } else {
            answerSheetMapper.updateById(sheet);
        }
        return buildWorkspace(plan, answerSheetMapper.selectById(sheet.getId()));
    }

    @Override
    public void reportEvent(Long examPlanId, CandidateEventReportRequest request, String username, String clientIp) {
        SysUser user = requireUser(username);
        requireCandidate(examPlanId, user.getId());
        AntiCheatEvent event = new AntiCheatEvent();
        event.setExamPlanId(examPlanId);
        event.setAnswerSheetId(request.getAnswerSheetId());
        event.setUserId(user.getId());
        event.setEventType(request.getEventType());
        event.setSeverity(request.getSeverity());
        event.setLeaveCount(request.getLeaveCount());
        event.setTriggeredAutoSave(request.getTriggeredAutoSave());
        event.setSaveVersion(request.getSaveVersion());
        event.setClientIp(resolveClientIp(clientIp));
        event.setDeviceFingerprint(trimToLength(request.getDeviceFingerprint(), 255));
        event.setDeviceInfo(trimToLength(request.getDeviceInfo(), 1000));
        event.setDetailText(request.getDetailText());
        event.setOccurredAt(LocalDateTime.now());
        antiCheatEventMapper.insert(event);
    }

    private CandidateExamWorkspaceVO buildWorkspace(ExamPlan plan, AnswerSheet sheet) {
        ExamPaper paper = requirePaper(plan.getPaperId());
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                .eq(PaperQuestion::getPaperId, plan.getPaperId())
                .orderByAsc(PaperQuestion::getSortNo, PaperQuestion::getId));
        Map<Long, QuestionBank> questionMap = loadQuestionMap(paperQuestions.stream().map(PaperQuestion::getQuestionId).toList());
        Map<Long, AnswerItem> answerMap = loadExistingAnswerItems(sheet.getId());

        List<PaperQuestion> displayQuestions = resolveDisplayQuestions(paperQuestions, paper, sheet);
        List<CandidateAnswerItemVO> items = new ArrayList<>();
        int displayOrder = 1;
        for (PaperQuestion paperQuestion : displayQuestions) {
            QuestionBank question = questionMap.get(paperQuestion.getQuestionId());
            AnswerItem answerItem = answerMap.get(paperQuestion.getQuestionId());
            items.add(CandidateAnswerItemVO.builder()
                    .answerItemId(answerItem == null ? null : answerItem.getId())
                    .questionId(paperQuestion.getQuestionId())
                    .questionOrder(displayOrder++)
                    .questionCode(question == null ? null : question.getQuestionCode())
                    .questionType(question == null ? null : question.getQuestionType())
                    .stem(question == null ? null : question.getStem())
                    .stemHtml(question == null ? null : question.getStemHtml())
                    .materialContent(question == null ? null : question.getMaterialContent())
                    .attachmentJson(question == null ? null : question.getAttachmentJson())
                    .optionsJson(question == null ? null : transformOptions(question.getOptionsJson(), paper, sheet, paperQuestion.getQuestionId()))
                    .maxScore(paperQuestion.getScore())
                    .answerContent(answerItem == null ? null : answerItem.getAnswerContent())
                    .scoreAwarded(answerItem == null ? null : answerItem.getScoreAwarded())
                    .status(answerItem == null ? "PENDING" : answerItem.getStatus())
                    .markedFlag(answerItem == null ? 0 : answerItem.getMarkedFlag())
                    .reviewLaterFlag(answerItem == null ? 0 : answerItem.getReviewLaterFlag())
                    .reviewComment(answerItem == null ? null : answerItem.getReviewComment())
                    .build());
        }

        LocalDateTime entryDeadlineAt = computeEntryDeadline(plan);
        LocalDateTime answerDeadlineAt = computeAnswerDeadline(plan, sheet);
        int remainingSeconds = (int) Math.max(0, Duration.between(LocalDateTime.now(), answerDeadlineAt).getSeconds());

        return CandidateExamWorkspaceVO.builder()
                .examPlanId(plan.getId())
                .examName(plan.getExamName())
                .paperName(plan.getPaperName())
                .subject(plan.getSubject())
                .instructionText(plan.getInstructionText())
                .startTime(plan.getStartTime())
                .endTime(plan.getEndTime())
                .entryDeadlineAt(entryDeadlineAt)
                .answerDeadlineAt(answerDeadlineAt)
                .durationMinutes(plan.getDurationMinutes())
                .remainingSeconds(remainingSeconds)
                .answerSheetId(sheet.getId())
                .answerSheetStatus(sheet.getStatus())
                .autoSubmitEnabled(plan.getAutoSubmitEnabled())
                .autoSubmitFlag(sheet.getAutoSubmitFlag())
                .saveVersion(sheet.getSaveVersion())
                .shuffleEnabled(paper.getShuffleEnabled())
                .paperVersion(paper.getPaperVersion())
                .antiCheatLevel(plan.getAntiCheatLevel())
                .antiCheatPolicy(buildAntiCheatPolicy(plan))
                .items(items)
                .build();
    }

    private void finalizeSubmission(ExamPlan plan,
                                    SysUser user,
                                    ExamCandidate candidate,
                                    AnswerSheet sheet,
                                    Map<Long, PaperQuestion> paperQuestionMap,
                                    Map<Long, QuestionBank> questionMap,
                                    Map<Long, AnswerItem> existingItems,
                                    boolean autoSubmit) {
        double objectiveScore = 0D;
        boolean hasSubjective = false;
        for (PaperQuestion paperQuestion : paperQuestionMap.values().stream().sorted(Comparator.comparing(PaperQuestion::getSortNo)).toList()) {
            QuestionBank question = questionMap.get(paperQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            AnswerItem answerItem = existingItems.getOrDefault(question.getId(), new AnswerItem());
            answerItem.setAnswerSheetId(sheet.getId());
            answerItem.setQuestionId(question.getId());
            answerItem.setQuestionType(question.getQuestionType());
            answerItem.setQuestionOrder(paperQuestion.getSortNo());
            answerItem.setMaxScore(paperQuestion.getScore());
            if (isObjective(question.getQuestionType())) {
                double awarded = normalizeAnswer(answerItem.getAnswerContent(), question.getQuestionType())
                        .equalsIgnoreCase(normalizeAnswer(question.getAnswerKey(), question.getQuestionType()))
                        ? paperQuestion.getScore() : 0D;
                answerItem.setScoreAwarded(awarded);
                answerItem.setAutoScored(1);
                answerItem.setMarkedFlag(1);
                answerItem.setReviewComment(autoSubmit ? "系统自动交卷后完成客观题判分" : "系统自动判分");
                answerItem.setStatus("AUTO_SCORED");
                objectiveScore += awarded;
            } else {
                hasSubjective = true;
                answerItem.setScoreAwarded(answerItem.getScoreAwarded() == null ? 0D : answerItem.getScoreAwarded());
                answerItem.setAutoScored(0);
                answerItem.setMarkedFlag(0);
                answerItem.setStatus("PENDING_GRADING");
            }

            if (answerItem.getId() == null) {
                answerItemMapper.insert(answerItem);
            } else {
                answerItemMapper.updateById(answerItem);
            }
        }

        sheet.setSubmittedAt(sheet.getSubmittedAt() == null ? LocalDateTime.now() : sheet.getSubmittedAt());
        sheet.setObjectiveScore(objectiveScore);
        sheet.setSubjectiveScore(0D);
        sheet.setFinalScore(objectiveScore);
        sheet.setStatus(hasSubjective ? "SUBMITTED" : "GRADED");
        sheet.setAutoSubmitFlag(autoSubmit ? 1 : 0);
        answerSheetMapper.updateById(sheet);

        candidate.setStatus(hasSubjective ? "SUBMITTED" : "COMPLETED");
        candidate.setAttemptCount((candidate.getAttemptCount() == null ? 0 : candidate.getAttemptCount()) + 1);
        examCandidateMapper.updateById(candidate);

        ExamRecord record = examRecordMapper.selectOne(Wrappers.lambdaQuery(ExamRecord.class)
                .eq(ExamRecord::getAnswerSheetId, sheet.getId())
                .last("limit 1"));
        if (record == null) {
            record = new ExamRecord();
            record.setAnswerSheetId(sheet.getId());
            record.setExamPlanId(plan.getId());
            record.setUserId(user.getId());
        }
        record.setCandidateName(sheet.getCandidateName());
        record.setExamName(plan.getExamName());
        record.setPaperName(plan.getPaperName());
        record.setSubmittedAt(sheet.getSubmittedAt());
        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(0D);
        record.setFinalScore(objectiveScore);
        record.setPassedFlag(objectiveScore >= plan.getPassScore() ? 1 : 0);
        record.setReviewStatus(hasSubjective ? "PENDING" : "APPROVED");
        if (!StringUtils.hasText(record.getAppealStatus())) {
            record.setAppealStatus("NONE");
        }
        record.setPublishedFlag(hasSubjective ? 0 : 1);
        record.setStatus(hasSubjective ? "PENDING_GRADING" : "PUBLISHED");
        if (record.getId() == null) {
            examRecordMapper.insert(record);
        } else {
            examRecordMapper.updateById(record);
        }

        AuditLog log = new AuditLog();
        log.setOperatorId(user.getId());
        log.setOperatorName(sheet.getCandidateName());
        log.setModuleName("CANDIDATE_EXAM");
        log.setActionName(autoSubmit ? "AUTO_SUBMIT" : "SUBMIT");
        log.setTargetType("ANSWER_SHEET");
        log.setTargetId(sheet.getId());
        log.setDetailText(autoSubmit ? "系统已根据考试时长自动交卷：" + plan.getExamName() : "考生提交考试：" + plan.getExamName());
        auditLogMapper.insert(log);
    }

    private AnswerSheet autoSubmitIfNeeded(ExamPlan plan, SysUser user, ExamCandidate candidate, AnswerSheet sheet) {
        if (sheet.getStartedAt() == null || isFinished(sheet) || !isAutoSubmitEnabled(plan)) {
            return sheet;
        }
        if (!LocalDateTime.now().isAfter(computeAnswerDeadline(plan, sheet))) {
            return sheet;
        }

        Map<Long, PaperQuestion> paperQuestionMap = loadPaperQuestionMap(plan.getPaperId());
        Map<Long, QuestionBank> questionMap = loadQuestionMap(paperQuestionMap.keySet());
        Map<Long, AnswerItem> existingItems = loadExistingAnswerItems(sheet.getId());
        sheet.setSaveVersion((sheet.getSaveVersion() == null ? 0 : sheet.getSaveVersion()) + 1);
        finalizeSubmission(plan, user, candidate, sheet, paperQuestionMap, questionMap, existingItems, true);
        return answerSheetMapper.selectById(sheet.getId());
    }

    private void upsertAnswerItems(AnswerSheet sheet,
                                   List<CandidateAnswerItemRequest> answers,
                                   Map<Long, PaperQuestion> paperQuestionMap,
                                   Map<Long, QuestionBank> questionMap,
                                   Map<Long, AnswerItem> existingItems) {
        for (CandidateAnswerItemRequest itemRequest : answers) {
            PaperQuestion paperQuestion = paperQuestionMap.get(itemRequest.getQuestionId());
            QuestionBank question = questionMap.get(itemRequest.getQuestionId());
            if (paperQuestion == null || question == null) {
                throw new BusinessException(4004, "题目不在当前试卷内，无法保存");
            }
            AnswerItem answerItem = existingItems.getOrDefault(itemRequest.getQuestionId(), new AnswerItem());
            answerItem.setAnswerSheetId(sheet.getId());
            answerItem.setQuestionId(question.getId());
            answerItem.setQuestionType(question.getQuestionType());
            answerItem.setQuestionOrder(paperQuestion.getSortNo());
            answerItem.setAnswerContent(itemRequest.getAnswerContent());
            answerItem.setMaxScore(paperQuestion.getScore());
            answerItem.setReviewLaterFlag(itemRequest.getReviewLaterFlag() == null ? 0 : itemRequest.getReviewLaterFlag());
            if (answerItem.getId() == null) {
                answerItem.setStatus("PENDING");
                answerItemMapper.insert(answerItem);
            } else {
                answerItemMapper.updateById(answerItem);
            }
            existingItems.put(itemRequest.getQuestionId(), answerItem);
        }
    }

    private Map<Long, PaperQuestion> loadPaperQuestionMap(Long paperId) {
        return paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                        .eq(PaperQuestion::getPaperId, paperId))
                .stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, Function.identity()));
    }

    private Map<Long, QuestionBank> loadQuestionMap(Iterable<Long> questionIds) {
        List<Long> ids = new ArrayList<>();
        questionIds.forEach(ids::add);
        return questionBankMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
    }

    private Map<Long, AnswerItem> loadExistingAnswerItems(Long answerSheetId) {
        return answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                        .eq(AnswerItem::getAnswerSheetId, answerSheetId))
                .stream()
                .collect(Collectors.toMap(AnswerItem::getQuestionId, Function.identity(), (left, right) -> right, HashMap::new));
    }

    private AnswerSheet getOrCreateSheet(ExamPlan plan, SysUser user, boolean markStarted) {
        AnswerSheet existing = answerSheetMapper.selectOne(Wrappers.lambdaQuery(AnswerSheet.class)
                .eq(AnswerSheet::getExamPlanId, plan.getId())
                .eq(AnswerSheet::getUserId, user.getId())
                .last("limit 1"));
        if (existing != null) {
            if (markStarted && existing.getStartedAt() == null) {
                existing.setStartedAt(LocalDateTime.now());
                existing.setStatus("IN_PROGRESS");
                answerSheetMapper.updateById(existing);
            }
            return existing;
        }
        AnswerSheet sheet = new AnswerSheet();
        sheet.setExamPlanId(plan.getId());
        sheet.setPaperId(plan.getPaperId());
        sheet.setPaperName(plan.getPaperName());
        sheet.setUserId(user.getId());
        sheet.setCandidateName(user.getFullName() == null ? user.getNickname() : user.getFullName());
        sheet.setStartedAt(markStarted ? LocalDateTime.now() : null);
        sheet.setStatus(markStarted ? "IN_PROGRESS" : "NOT_STARTED");
        sheet.setObjectiveScore(0D);
        sheet.setSubjectiveScore(0D);
        sheet.setFinalScore(0D);
        sheet.setAutoSubmitFlag(0);
        sheet.setSaveVersion(0);
        answerSheetMapper.insert(sheet);
        return sheet;
    }

    private AnswerSheet markSheetStarted(AnswerSheet sheet) {
        sheet.setStartedAt(LocalDateTime.now());
        sheet.setStatus("IN_PROGRESS");
        answerSheetMapper.updateById(sheet);
        return answerSheetMapper.selectById(sheet.getId());
    }

    private void ensurePlanOpen(ExamPlan plan, AnswerSheet sheet) {
        LocalDateTime now = LocalDateTime.now();
        if (plan.getPublishStatus() == null || plan.getPublishStatus() != 1) {
            throw new BusinessException(4005, "当前考试尚未发布");
        }
        if (now.isBefore(plan.getStartTime())) {
            throw new BusinessException(4006, "考试尚未开始，请在允许时间内进入");
        }
        if (sheet.getStartedAt() == null && now.isAfter(computeEntryDeadline(plan))) {
            throw new BusinessException(4006, "考试允许进入的时间窗口已结束");
        }
    }

    private void ensureAttemptAllowed(ExamPlan plan, ExamCandidate candidate, AnswerSheet sheet) {
        boolean finished = isFinished(sheet);
        if (finished && candidate.getAttemptCount() != null && plan.getAttemptLimit() != null && candidate.getAttemptCount() >= plan.getAttemptLimit()) {
            throw new BusinessException(4005, "你已达到本场考试允许的参考次数");
        }
    }

    private void ensurePasswordAllowed(ExamPlan plan, AnswerSheet sheet, String examPassword) {
        if (plan.getExamPassword() == null || plan.getExamPassword().isBlank() || sheet.getStartedAt() != null) {
            return;
        }
        if (examPassword == null || !plan.getExamPassword().equals(examPassword)) {
            throw new BusinessException(4005, "考试口令不正确，无法进入考试");
        }
    }

    private void ensureEarlySubmitAllowed(ExamPlan plan, AnswerSheet sheet) {
        if (sheet.getStartedAt() == null || plan.getEarlySubmitMinutes() == null || plan.getEarlySubmitMinutes() <= 0) {
            return;
        }
        if (LocalDateTime.now().isBefore(sheet.getStartedAt().plusMinutes(plan.getEarlySubmitMinutes()))) {
            throw new BusinessException(4005, "当前尚未达到允许提前交卷的时间");
        }
    }

    private LocalDateTime computeEntryDeadline(ExamPlan plan) {
        if (plan.getLateEntryMinutes() != null && plan.getLateEntryMinutes() > 0) {
            LocalDateTime lateDeadline = plan.getStartTime().plusMinutes(plan.getLateEntryMinutes());
            return lateDeadline.isBefore(plan.getEndTime()) ? lateDeadline : plan.getEndTime();
        }
        return plan.getEndTime();
    }

    private LocalDateTime computeAnswerDeadline(ExamPlan plan, AnswerSheet sheet) {
        if (sheet.getStartedAt() == null) {
            return computeEntryDeadline(plan);
        }
        LocalDateTime durationDeadline = sheet.getStartedAt().plusMinutes(plan.getDurationMinutes() == null ? 0 : plan.getDurationMinutes());
        return durationDeadline.isBefore(plan.getEndTime()) ? durationDeadline : plan.getEndTime();
    }

    private List<PaperQuestion> resolveDisplayQuestions(List<PaperQuestion> paperQuestions, ExamPaper paper, AnswerSheet sheet) {
        if (paper.getShuffleEnabled() == null || paper.getShuffleEnabled() != 1) {
            return paperQuestions.stream()
                    .sorted(Comparator.comparing(PaperQuestion::getSortNo).thenComparing(PaperQuestion::getId))
                    .toList();
        }
        return paperQuestions.stream()
                .sorted(Comparator.comparingInt(question -> stableWeight(sheet.getId(), question.getQuestionId())))
                .toList();
    }

    private String transformOptions(String optionsJson, ExamPaper paper, AnswerSheet sheet, Long questionId) {
        if (!StringUtils.hasText(optionsJson) || paper.getShuffleEnabled() == null || paper.getShuffleEnabled() != 1) {
            return optionsJson;
        }
        try {
            List<String> options = objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
            return objectMapper.writeValueAsString(options.stream()
                    .sorted(Comparator.comparingInt(option -> stableWeight(sheet.getId() + questionId, option.hashCode() * 1L)))
                    .toList());
        } catch (JsonProcessingException exception) {
            return optionsJson;
        }
    }

    private int stableWeight(Long seed, Long value) {
        return Math.abs(Objects.hash(seed, value));
    }

    private boolean isAutoSubmitEnabled(ExamPlan plan) {
        return plan.getAutoSubmitEnabled() == null || plan.getAutoSubmitEnabled() == 1;
    }

    private void ensureSingleDeviceAllowed(ExamPlan plan, SysUser user, AnswerSheet sheet, String deviceFingerprint) {
        if (!configBoolean("exam.anti.cheat.single-device.enabled", true)) {
            return;
        }
        if ("BASIC".equalsIgnoreCase(plan.getAntiCheatLevel())) {
            return;
        }
        if (!StringUtils.hasText(deviceFingerprint) || isFinished(sheet)) {
            return;
        }
        List<AntiCheatEvent> deviceEvents = antiCheatEventMapper.selectList(Wrappers.lambdaQuery(AntiCheatEvent.class)
                .eq(AntiCheatEvent::getExamPlanId, plan.getId())
                .eq(AntiCheatEvent::getUserId, user.getId())
                .eq(AntiCheatEvent::getEventType, "DEVICE_CONTEXT")
                .isNotNull(AntiCheatEvent::getDeviceFingerprint)
                .orderByDesc(AntiCheatEvent::getOccurredAt, AntiCheatEvent::getId));
        boolean hasOtherDevice = deviceEvents.stream()
                .map(AntiCheatEvent::getDeviceFingerprint)
                .filter(StringUtils::hasText)
                .anyMatch(existing -> !existing.equals(deviceFingerprint));
        if (hasOtherDevice) {
            throw new BusinessException(4005, "当前考试已在另一台设备打开，基础单设备限制已阻止本次进入");
        }
    }

    private AntiCheatPolicyVO buildAntiCheatPolicy(ExamPlan plan) {
        boolean strictMode = !"BASIC".equalsIgnoreCase(plan.getAntiCheatLevel());
        return AntiCheatPolicyVO.builder()
                .blockCopyEnabled(flag(strictMode && configBoolean("exam.anti.cheat.block.copy.enabled", true)))
                .blockPasteEnabled(flag(strictMode && configBoolean("exam.anti.cheat.block.paste.enabled", true)))
                .blockContextMenuEnabled(flag(strictMode && configBoolean("exam.anti.cheat.block.context-menu.enabled", true)))
                .blockShortcutEnabled(flag(strictMode && configBoolean("exam.anti.cheat.block.shortcuts.enabled", true)))
                .deviceLoggingEnabled(flag(configBoolean("exam.anti.cheat.device-logging.enabled", true)))
                .deviceCheckEnabled(flag(strictMode && configBoolean("exam.anti.cheat.device-check.enabled", true)))
                .blockOnDeviceCheckFail(flag(strictMode && configBoolean("exam.anti.cheat.device-check.block-on-fail", true)))
                .forbidMobileEntry(flag(strictMode && configBoolean("exam.anti.cheat.device-check.forbid-mobile", true)))
                .requireFullscreenSupport(flag(strictMode && configBoolean("exam.anti.cheat.device-check.require-fullscreen-support", true)))
                .minWindowWidth(configInt("exam.anti.cheat.device-check.min-window-width", 1200))
                .minWindowHeight(configInt("exam.anti.cheat.device-check.min-window-height", 700))
                .blockedShortcutKeys(parseCsvConfig("exam.anti.cheat.block.shortcuts.keys",
                        "F5,Ctrl+R,Meta+R,Ctrl+Shift+I,F12,Ctrl+Shift+C,Ctrl+U"))
                .allowedBrowserKeywords(parseCsvConfig("exam.anti.cheat.device-check.allowed-browsers",
                        "Chrome,Edg"))
                .build();
    }

    private boolean configBoolean(String key, boolean defaultValue) {
        String value = configValue(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private List<String> parseCsvConfig(String key, String defaultValue) {
        String source = configValue(key);
        String raw = StringUtils.hasText(source) ? source : defaultValue;
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private Integer configInt(String key, int defaultValue) {
        String value = configValue(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String configValue(String key) {
        ConfigItem item = configItemMapper.selectOne(Wrappers.lambdaQuery(ConfigItem.class)
                .eq(ConfigItem::getConfigKey, key)
                .eq(ConfigItem::getStatus, 1)
                .last("limit 1"));
        return item == null ? null : item.getConfigValue();
    }

    private Integer flag(boolean enabled) {
        return enabled ? 1 : 0;
    }

    private String resolveClientIp(String rawIp) {
        if (!StringUtils.hasText(rawIp)) {
            return "unknown";
        }
        return trimToLength(rawIp, 64);
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isFinished(AnswerSheet sheet) {
        return List.of("SUBMITTED", "PARTIALLY_GRADED", "REVIEW_PENDING", "REJUDGING", "PUBLISHED").contains(sheet.getStatus());
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

    private ExamPlan requirePlan(Long examPlanId) {
        ExamPlan plan = examPlanMapper.selectById(examPlanId);
        if (plan == null) {
            throw new BusinessException(4040, "考试计划不存在");
        }
        return plan;
    }

    private ExamPaper requirePaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(4041, "试卷不存在");
        }
        return paper;
    }

    private ExamCandidate requireCandidate(Long examPlanId, Long userId) {
        ExamCandidate candidate = examCandidateMapper.selectOne(Wrappers.lambdaQuery(ExamCandidate.class)
                .eq(ExamCandidate::getExamPlanId, examPlanId)
                .eq(ExamCandidate::getUserId, userId)
                .last("limit 1"));
        if (candidate == null) {
            throw new BusinessException(4030, "当前账号未被分配到该考试");
        }
        return candidate;
    }

    private boolean isObjective(String questionType) {
        return List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "JUDGE", "FILL_BLANK").contains(String.valueOf(questionType).toUpperCase(Locale.ROOT));
    }

    private String normalizeAnswer(String answer, String questionType) {
        if (answer == null) {
            return "";
        }
        if ("MULTIPLE_CHOICE".equalsIgnoreCase(questionType)) {
            return List.of(answer.split("\\|")).stream()
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .sorted()
                    .collect(Collectors.joining("|"));
        }
        if ("FILL_BLANK".equalsIgnoreCase(questionType)) {
            return List.of(answer.split("\\|")).stream()
                    .map(String::trim)
                    .collect(Collectors.joining("|"));
        }
        return answer.trim();
    }
}
