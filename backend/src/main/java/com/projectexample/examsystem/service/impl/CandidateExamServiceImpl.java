package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.CandidateAnswerItemRequest;
import com.projectexample.examsystem.dto.CandidateAnswerSheetSaveRequest;
import com.projectexample.examsystem.dto.CandidateEventReportRequest;
import com.projectexample.examsystem.entity.AnswerItem;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.AntiCheatEvent;
import com.projectexample.examsystem.entity.AuditLog;
import com.projectexample.examsystem.entity.ExamCandidate;
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
import com.projectexample.examsystem.mapper.ExamCandidateMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.PaperQuestionMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.service.CandidateExamService;
import com.projectexample.examsystem.vo.CandidateAnswerItemVO;
import com.projectexample.examsystem.vo.CandidateExamVO;
import com.projectexample.examsystem.vo.CandidateExamWorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateExamServiceImpl implements CandidateExamService {

    private final SysUserMapper sysUserMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamCandidateMapper examCandidateMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final AnswerItemMapper answerItemMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AuditLogMapper auditLogMapper;
    private final AntiCheatEventMapper antiCheatEventMapper;

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
                    return CandidateExamVO.builder()
                            .examPlanId(plan.getId())
                            .examName(plan.getExamName())
                            .paperName(plan.getPaperName())
                            .subject(plan.getSubject())
                            .startTime(plan.getStartTime())
                            .endTime(plan.getEndTime())
                            .candidateStatus(candidate.getStatus())
                            .attemptCount(candidate.getAttemptCount())
                            .answerSheetStatus(sheet == null ? "NOT_STARTED" : sheet.getStatus())
                            .build();
                })
                .toList();
    }

    @Override
    public CandidateExamWorkspaceVO getWorkspace(Long examPlanId, String username) {
        SysUser user = requireUser(username);
        ExamPlan plan = requirePlan(examPlanId);
        requireCandidate(plan.getId(), user.getId());
        ensurePlanOpen(plan);
        return buildWorkspace(plan, getOrCreateSheet(plan, user, false));
    }

    @Override
    public CandidateExamWorkspaceVO saveAnswers(Long examPlanId, CandidateAnswerSheetSaveRequest request, boolean submit, String username) {
        SysUser user = requireUser(username);
        ExamPlan plan = requirePlan(examPlanId);
        ExamCandidate candidate = requireCandidate(plan.getId(), user.getId());
        ensurePlanOpen(plan);

        AnswerSheet sheet = getOrCreateSheet(plan, user, true);
        Map<Long, PaperQuestion> paperQuestionMap = paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                        .eq(PaperQuestion::getPaperId, plan.getPaperId()))
                .stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, Function.identity()));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(paperQuestionMap.keySet()).stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        Map<Long, AnswerItem> existingItems = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                        .eq(AnswerItem::getAnswerSheetId, sheet.getId()))
                .stream()
                .collect(Collectors.toMap(AnswerItem::getQuestionId, Function.identity(), (left, right) -> right, HashMap::new));

        for (CandidateAnswerItemRequest itemRequest : request.getAnswers()) {
            PaperQuestion paperQuestion = paperQuestionMap.get(itemRequest.getQuestionId());
            QuestionBank question = questionMap.get(itemRequest.getQuestionId());
            if (paperQuestion == null || question == null) {
                throw new BusinessException(4004, "Question is not part of the current paper");
            }
            AnswerItem answerItem = existingItems.getOrDefault(itemRequest.getQuestionId(), new AnswerItem());
            answerItem.setAnswerSheetId(sheet.getId());
            answerItem.setQuestionId(question.getId());
            answerItem.setQuestionType(question.getQuestionType());
            answerItem.setQuestionOrder(paperQuestion.getSortNo());
            answerItem.setAnswerContent(itemRequest.getAnswerContent());
            answerItem.setMaxScore(paperQuestion.getScore());
            if (answerItem.getId() == null) {
                answerItem.setStatus("PENDING");
                answerItemMapper.insert(answerItem);
            } else {
                answerItemMapper.updateById(answerItem);
            }
            existingItems.put(itemRequest.getQuestionId(), answerItem);
        }

        sheet.setSaveVersion((sheet.getSaveVersion() == null ? 0 : sheet.getSaveVersion()) + 1);
        sheet.setStatus(submit ? "SUBMITTED" : "IN_PROGRESS");
        if (submit) {
            finalizeSubmission(plan, user, candidate, sheet, paperQuestionMap, questionMap, existingItems);
        } else {
            answerSheetMapper.updateById(sheet);
        }
        return buildWorkspace(plan, answerSheetMapper.selectById(sheet.getId()));
    }

    @Override
    public void reportEvent(Long examPlanId, CandidateEventReportRequest request, String username) {
        SysUser user = requireUser(username);
        requireCandidate(examPlanId, user.getId());
        AntiCheatEvent event = new AntiCheatEvent();
        event.setExamPlanId(examPlanId);
        event.setAnswerSheetId(request.getAnswerSheetId());
        event.setUserId(user.getId());
        event.setEventType(request.getEventType());
        event.setSeverity(request.getSeverity());
        event.setDetailText(request.getDetailText());
        event.setOccurredAt(LocalDateTime.now());
        antiCheatEventMapper.insert(event);
    }

    private void finalizeSubmission(ExamPlan plan, SysUser user, ExamCandidate candidate, AnswerSheet sheet,
                                    Map<Long, PaperQuestion> paperQuestionMap, Map<Long, QuestionBank> questionMap,
                                    Map<Long, AnswerItem> existingItems) {
        double objectiveScore = 0D;
        boolean hasSubjective = false;
        for (PaperQuestion paperQuestion : paperQuestionMap.values().stream().sorted(Comparator.comparing(PaperQuestion::getSortNo)).toList()) {
            QuestionBank question = questionMap.get(paperQuestion.getQuestionId());
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
                answerItem.setReviewComment("Auto scored");
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

        sheet.setSubmittedAt(LocalDateTime.now());
        sheet.setObjectiveScore(objectiveScore);
        sheet.setSubjectiveScore(0D);
        sheet.setFinalScore(objectiveScore);
        sheet.setStatus(hasSubjective ? "SUBMITTED" : "GRADED");
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
        log.setActionName("SUBMIT");
        log.setTargetType("ANSWER_SHEET");
        log.setTargetId(sheet.getId());
        log.setDetailText("Candidate submitted exam " + plan.getExamName());
        auditLogMapper.insert(log);
    }

    private CandidateExamWorkspaceVO buildWorkspace(ExamPlan plan, AnswerSheet sheet) {
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(Wrappers.lambdaQuery(PaperQuestion.class)
                .eq(PaperQuestion::getPaperId, plan.getPaperId())
                .orderByAsc(PaperQuestion::getSortNo, PaperQuestion::getId));
        Map<Long, QuestionBank> questionMap = questionBankMapper.selectBatchIds(paperQuestions.stream().map(PaperQuestion::getQuestionId).toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        Map<Long, AnswerItem> answerMap = answerItemMapper.selectList(Wrappers.lambdaQuery(AnswerItem.class)
                        .eq(AnswerItem::getAnswerSheetId, sheet.getId()))
                .stream()
                .collect(Collectors.toMap(AnswerItem::getQuestionId, Function.identity(), (left, right) -> right));

        List<CandidateAnswerItemVO> items = paperQuestions.stream()
                .map(paperQuestion -> {
                    QuestionBank question = questionMap.get(paperQuestion.getQuestionId());
                    AnswerItem answerItem = answerMap.get(paperQuestion.getQuestionId());
                    return CandidateAnswerItemVO.builder()
                            .answerItemId(answerItem == null ? null : answerItem.getId())
                            .questionId(paperQuestion.getQuestionId())
                            .questionOrder(paperQuestion.getSortNo())
                            .questionCode(question == null ? null : question.getQuestionCode())
                            .questionType(question == null ? null : question.getQuestionType())
                            .stem(question == null ? null : question.getStem())
                            .optionsJson(question == null ? null : question.getOptionsJson())
                            .maxScore(paperQuestion.getScore())
                            .answerContent(answerItem == null ? null : answerItem.getAnswerContent())
                            .scoreAwarded(answerItem == null ? null : answerItem.getScoreAwarded())
                            .status(answerItem == null ? "PENDING" : answerItem.getStatus())
                            .markedFlag(answerItem == null ? 0 : answerItem.getMarkedFlag())
                            .reviewComment(answerItem == null ? null : answerItem.getReviewComment())
                            .build();
                })
                .toList();

        return CandidateExamWorkspaceVO.builder()
                .examPlanId(plan.getId())
                .examName(plan.getExamName())
                .paperName(plan.getPaperName())
                .subject(plan.getSubject())
                .instructionText(plan.getInstructionText())
                .startTime(plan.getStartTime())
                .endTime(plan.getEndTime())
                .durationMinutes(plan.getDurationMinutes())
                .answerSheetId(sheet.getId())
                .answerSheetStatus(sheet.getStatus())
                .items(items)
                .build();
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

    private SysUser requireUser(String username) {
        SysUser user = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, username).last("limit 1"));
        if (user == null) {
            throw new BusinessException(4010, "User session is invalid");
        }
        return user;
    }

    private ExamPlan requirePlan(Long examPlanId) {
        ExamPlan plan = examPlanMapper.selectById(examPlanId);
        if (plan == null) {
            throw new BusinessException(4040, "Exam plan not found");
        }
        return plan;
    }

    private ExamCandidate requireCandidate(Long examPlanId, Long userId) {
        ExamCandidate candidate = examCandidateMapper.selectOne(Wrappers.lambdaQuery(ExamCandidate.class)
                .eq(ExamCandidate::getExamPlanId, examPlanId)
                .eq(ExamCandidate::getUserId, userId)
                .last("limit 1"));
        if (candidate == null) {
            throw new BusinessException(4030, "Current user is not assigned to this exam");
        }
        return candidate;
    }

    private void ensurePlanOpen(ExamPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        if (plan.getPublishStatus() == null || plan.getPublishStatus() != 1) {
            throw new BusinessException(4005, "This exam is not published");
        }
        if (now.isBefore(plan.getStartTime())) {
            throw new BusinessException(4006, "This exam has not started yet");
        }
        if (now.isAfter(plan.getEndTime())) {
            throw new BusinessException(4007, "This exam is already closed");
        }
    }

    private boolean isObjective(String questionType) {
        return List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "JUDGE").contains(questionType);
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
        return answer.trim();
    }
}
