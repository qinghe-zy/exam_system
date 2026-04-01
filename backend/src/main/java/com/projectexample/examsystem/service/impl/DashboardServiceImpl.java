package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamPaper;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.ExamPaperMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysMenuMapper;
import com.projectexample.examsystem.mapper.SysRoleMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.service.DashboardService;
import com.projectexample.examsystem.vo.DashboardMetricVO;
import com.projectexample.examsystem.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final QuestionBankMapper questionBankMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerSheetMapper answerSheetMapper;

    @Override
    public DashboardOverviewVO getOverview() {
        return DashboardOverviewVO.builder()
                .headline("The online examination monolith now spans plan, answer, grading, and score publication")
                .summary("The current system stays as a clear single application, but now extends beyond CRUD into exam publication, candidate answering, grading, score output, and anti-cheat event capture.")
                .metrics(List.of(
                        new DashboardMetricVO("Users", sysUserMapper.selectCount(null), "Available authenticated users"),
                        new DashboardMetricVO("Questions", questionBankMapper.selectCount(null), "Question bank records"),
                        new DashboardMetricVO("Published Papers", examPaperMapper.selectCount(Wrappers.lambdaQuery(ExamPaper.class).eq(ExamPaper::getPublishStatus, 1)), "Ready-to-use papers"),
                        new DashboardMetricVO("Published Exams", examPlanMapper.selectCount(Wrappers.lambdaQuery(ExamPlan.class).eq(ExamPlan::getPublishStatus, 1)), "Live exam plans"),
                        new DashboardMetricVO("Pending Grading", answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class).in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED"))), "Submitted sheets waiting for grading")
                ))
                .nextActions(List.of(
                        "Publish exam plans only after papers and candidate rosters are ready.",
                        "Review pending grading tasks and proctor events during live operation windows.",
                        "Use the analytics module after grading to track pass rate and score spread."
                ))
                .build();
    }
}
