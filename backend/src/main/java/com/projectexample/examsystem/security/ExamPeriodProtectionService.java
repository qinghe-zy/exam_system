package com.projectexample.examsystem.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExamPeriodProtectionService {

    private final ExamPlanMapper examPlanMapper;

    public void assertMutable(String actionName) {
        if (hasActiveExam()) {
            throw new BusinessException(4005, "当前存在进行中的考试，暂不允许" + actionName);
        }
    }

    public boolean hasActiveExam() {
        LocalDateTime now = LocalDateTime.now();
        Long count = examPlanMapper.selectCount(Wrappers.lambdaQuery(ExamPlan.class)
                .eq(ExamPlan::getPublishStatus, 1)
                .eq(ExamPlan::getStatus, 1)
                .isNotNull(ExamPlan::getStartTime)
                .isNotNull(ExamPlan::getEndTime)
                .le(ExamPlan::getStartTime, now)
                .ge(ExamPlan::getEndTime, now));
        return count != null && count > 0;
    }
}
