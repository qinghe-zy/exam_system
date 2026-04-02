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
                .headline("当前平台已形成覆盖发布、作答、阅卷与成绩发布的考试闭环")
                .summary("系统保持清晰单体架构，并在此基础上扩展了题库、试卷、考试计划、考生作答、阅卷评分、成绩分析与基础监考事件留痕。")
                .metrics(List.of(
                        new DashboardMetricVO("用户总数", sysUserMapper.selectCount(null), "当前系统中的可登录账号数量"),
                        new DashboardMetricVO("题目总数", questionBankMapper.selectCount(null), "当前题库中的题目数量"),
                        new DashboardMetricVO("已发布试卷", examPaperMapper.selectCount(Wrappers.lambdaQuery(ExamPaper.class).eq(ExamPaper::getPublishStatus, 1)), "当前可用于发布考试的试卷数量"),
                        new DashboardMetricVO("已发布考试", examPlanMapper.selectCount(Wrappers.lambdaQuery(ExamPlan.class).eq(ExamPlan::getPublishStatus, 1)), "当前处于发布状态的考试计划数量"),
                        new DashboardMetricVO("待阅卷答卷", answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class).in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED"))), "当前等待阅卷的答卷数量")
                ))
                .nextActions(List.of(
                        "在发布考试前先确认试卷、考生名单和考试规则配置完整。",
                        "考试进行中重点关注待阅卷任务和监考事件。",
                        "考试结束后通过成绩分析模块查看分布、及格率和知识点掌握情况。"
                ))
                .build();
    }
}
