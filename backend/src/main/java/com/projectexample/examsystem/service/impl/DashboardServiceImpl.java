package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ExamPaper;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.AntiCheatEventMapper;
import com.projectexample.examsystem.mapper.ExamCandidateMapper;
import com.projectexample.examsystem.mapper.ExamPaperMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.mapper.InAppMessageMapper;
import com.projectexample.examsystem.mapper.QuestionBankMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
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
    private final QuestionBankMapper questionBankMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamRecordMapper examRecordMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final ExamCandidateMapper examCandidateMapper;
    private final InAppMessageMapper inAppMessageMapper;
    private final AntiCheatEventMapper antiCheatEventMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public DashboardOverviewVO getOverview() {
        SysUser currentUser = accessScopeService.currentUser();
        if ("STUDENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            return buildStudentOverview(currentUser);
        }
        return buildScopedOverview();
    }

    private DashboardOverviewVO buildScopedOverview() {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        List<Long> scopedPlanIds = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                        .in(!accessScopeService.isAdmin(), ExamPlan::getOrganizationId, accessibleOrgIds.isEmpty() ? List.of(-1L) : accessibleOrgIds))
                .stream()
                .map(ExamPlan::getId)
                .toList();

        return DashboardOverviewVO.builder()
                .headline("当前平台已形成覆盖发布、作答、阅卷与成绩发布的考试闭环")
                .summary("当前看板数据已按账号可访问组织范围进行过滤，用于避免越权查看全局业务指标。")
                .metrics(List.of(
                        new DashboardMetricVO("用户总数", sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                                .in(!accessScopeService.isAdmin(), SysUser::getOrganizationId, accessibleOrgIds.isEmpty() ? List.of(-1L) : accessibleOrgIds)), "当前可访问组织内的可登录账号数量"),
                        new DashboardMetricVO("题目总数", questionBankMapper.selectCount(Wrappers.lambdaQuery(com.projectexample.examsystem.entity.QuestionBank.class)
                                .in(!accessScopeService.isAdmin(), com.projectexample.examsystem.entity.QuestionBank::getOrganizationId, accessibleOrgIds.isEmpty() ? List.of(-1L) : accessibleOrgIds)), "当前可访问组织内的题库题目数量"),
                        new DashboardMetricVO("已发布试卷", examPaperMapper.selectCount(Wrappers.lambdaQuery(ExamPaper.class)
                                .eq(ExamPaper::getPublishStatus, 1)
                                .in(!accessScopeService.isAdmin(), ExamPaper::getOrganizationId, accessibleOrgIds.isEmpty() ? List.of(-1L) : accessibleOrgIds)), "当前可用于发布考试的试卷数量"),
                        new DashboardMetricVO("已发布考试", examPlanMapper.selectCount(Wrappers.lambdaQuery(ExamPlan.class)
                                .eq(ExamPlan::getPublishStatus, 1)
                                .in(!accessScopeService.isAdmin(), ExamPlan::getOrganizationId, accessibleOrgIds.isEmpty() ? List.of(-1L) : accessibleOrgIds)), "当前处于发布状态的考试计划数量"),
                        new DashboardMetricVO("待阅卷答卷", answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class)
                                .in(AnswerSheet::getStatus, List.of("SUBMITTED", "PARTIALLY_GRADED"))
                                .in(!accessScopeService.isAdmin(), AnswerSheet::getExamPlanId, scopedPlanIds.isEmpty() ? List.of(-1L) : scopedPlanIds)), "当前等待阅卷的答卷数量")
                ))
                .nextActions(List.of(
                        "发布考试前确认题库、试卷和考生范围均属于当前组织授权范围。",
                        "考试进行中重点查看待阅卷任务与监考事件，避免组织外数据混入视图。",
                        "考试结束后通过成绩分析模块查看本组织数据分布。"
                ))
                .build();
    }

    private DashboardOverviewVO buildStudentOverview(SysUser currentUser) {
        return DashboardOverviewVO.builder()
                .headline("当前账号已进入个人考试与成绩工作台")
                .summary("学生看板只展示当前账号自己的考试、成绩、消息和异常记录，不再展示全局运营指标。")
                .metrics(List.of(
                        new DashboardMetricVO("待考场次", examCandidateMapper.selectCount(Wrappers.lambdaQuery(com.projectexample.examsystem.entity.ExamCandidate.class)
                                .eq(com.projectexample.examsystem.entity.ExamCandidate::getUserId, currentUser.getId())
                                .eq(com.projectexample.examsystem.entity.ExamCandidate::getStatus, "ASSIGNED")), "当前账号尚未进入或完成的考试场次"),
                        new DashboardMetricVO("已发布成绩", examRecordMapper.selectCount(Wrappers.lambdaQuery(com.projectexample.examsystem.entity.ExamRecord.class)
                                .eq(com.projectexample.examsystem.entity.ExamRecord::getUserId, currentUser.getId())
                                .eq(com.projectexample.examsystem.entity.ExamRecord::getPublishedFlag, 1)), "当前账号可查看的已发布成绩数量"),
                        new DashboardMetricVO("未读消息", inAppMessageMapper.selectCount(Wrappers.lambdaQuery(com.projectexample.examsystem.entity.InAppMessage.class)
                                .eq(com.projectexample.examsystem.entity.InAppMessage::getRecipientUserId, currentUser.getId())
                                .eq(com.projectexample.examsystem.entity.InAppMessage::getReadFlag, 0)), "当前账号尚未处理的站内消息"),
                        new DashboardMetricVO("异常留痕", antiCheatEventMapper.selectCount(Wrappers.lambdaQuery(com.projectexample.examsystem.entity.AntiCheatEvent.class)
                                .eq(com.projectexample.examsystem.entity.AntiCheatEvent::getUserId, currentUser.getId())), "当前账号在考试中触发的异常行为留痕数量")
                ))
                .nextActions(List.of(
                        "在进入考试前先查看待考场次和开考时间。",
                        "考试结束后可到“我的成绩”查看已发布结果与逐题详情。",
                        "如消息中心出现成绩提醒或考试提醒，请及时处理。"
                ))
                .build();
    }
}
