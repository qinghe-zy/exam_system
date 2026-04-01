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
import com.projectexample.examsystem.service.ExamPlanService;
import com.projectexample.examsystem.vo.ExamPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamPlanServiceImpl implements ExamPlanService {

    private final ExamPlanMapper examPlanMapper;
    private final ExamPaperMapper examPaperMapper;
    private final SysUserMapper sysUserMapper;
    private final ExamCandidateMapper examCandidateMapper;
    private final AnswerSheetMapper answerSheetMapper;

    @Override
    public List<ExamPlanVO> listPlans() {
        return examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class).orderByDesc(ExamPlan::getStartTime, ExamPlan::getId))
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
        return toVO(requirePlan(entity.getId()));
    }

    @Override
    public ExamPlanVO updatePlan(Long id, ExamPlanSaveRequest request) {
        ExamPlan entity = requirePlan(id);
        ExamPaper paper = requirePaper(request.getPaperId());
        apply(entity, request, paper);
        examPlanMapper.updateById(entity);
        replaceCandidates(id, request.getCandidateUserIds());
        return toVO(requirePlan(id));
    }

    @Override
    public void deletePlan(Long id) {
        requirePlan(id);
        examCandidateMapper.delete(Wrappers.lambdaQuery(ExamCandidate.class).eq(ExamCandidate::getExamPlanId, id));
        examPlanMapper.deleteById(id);
    }

    private ExamPlan requirePlan(Long id) {
        ExamPlan plan = examPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(4040, "Exam plan not found");
        }
        return plan;
    }

    private ExamPaper requirePaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(4041, "Exam paper not found");
        }
        return paper;
    }

    private void apply(ExamPlan entity, ExamPlanSaveRequest request, ExamPaper paper) {
        entity.setExamCode(request.getExamCode());
        entity.setExamName(request.getExamName());
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
        entity.setEarlySubmitMinutes(request.getEarlySubmitMinutes() == null ? 0 : request.getEarlySubmitMinutes());
        entity.setAutoSubmitEnabled(request.getAutoSubmitEnabled() == null ? 1 : request.getAutoSubmitEnabled());
        entity.setAntiCheatLevel(request.getAntiCheatLevel() == null ? "BASIC" : request.getAntiCheatLevel());
        entity.setInstructionText(request.getInstructionText());
        entity.setStatus(request.getStatus());
        entity.setPublishStatus(request.getPublishStatus());
    }

    private void replaceCandidates(Long examPlanId, List<Long> candidateUserIds) {
        examCandidateMapper.delete(Wrappers.lambdaQuery(ExamCandidate.class).eq(ExamCandidate::getExamPlanId, examPlanId));
        for (Long userId : candidateUserIds) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(4042, "Candidate user not found: " + userId);
            }
            ExamCandidate candidate = new ExamCandidate();
            candidate.setExamPlanId(examPlanId);
            candidate.setUserId(userId);
            candidate.setCandidateName(user.getFullName() == null ? user.getNickname() : user.getFullName());
            candidate.setOrganizationName(user.getOrganizationName());
            candidate.setStatus("ASSIGNED");
            candidate.setAttemptCount(0);
            candidate.setAccessCode("EX" + examPlanId + "-" + userId);
            examCandidateMapper.insert(candidate);
        }
    }

    private ExamPlanVO toVO(ExamPlan entity) {
        List<ExamCandidate> candidates = examCandidateMapper.selectList(Wrappers.lambdaQuery(ExamCandidate.class)
                .eq(ExamCandidate::getExamPlanId, entity.getId()));
        long submittedCount = answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class)
                .eq(AnswerSheet::getExamPlanId, entity.getId())
                .in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED", "GRADED")));
        return ExamPlanVO.builder()
                .id(entity.getId())
                .examCode(entity.getExamCode())
                .examName(entity.getExamName())
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
                .earlySubmitMinutes(entity.getEarlySubmitMinutes())
                .autoSubmitEnabled(entity.getAutoSubmitEnabled())
                .antiCheatLevel(entity.getAntiCheatLevel())
                .instructionText(entity.getInstructionText())
                .status(entity.getStatus())
                .publishStatus(entity.getPublishStatus())
                .candidateCount(candidates.size())
                .submittedCount((int) submittedCount)
                .candidateUserIds(candidates.stream().map(ExamCandidate::getUserId).toList())
                .build();
    }
}
