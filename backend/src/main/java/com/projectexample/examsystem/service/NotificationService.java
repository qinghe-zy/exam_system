package com.projectexample.examsystem.service;

import com.projectexample.examsystem.common.PageResponse;
import com.projectexample.examsystem.dto.NotificationDeliveryLogQueryRequest;
import com.projectexample.examsystem.dto.NotificationTemplateSaveRequest;
import com.projectexample.examsystem.entity.ExamCandidate;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.LoginRiskLog;
import com.projectexample.examsystem.entity.ScoreAppeal;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.vo.NotificationDeliveryLogVO;
import com.projectexample.examsystem.vo.NotificationTemplateVO;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {

    List<NotificationTemplateVO> listTemplates();

    NotificationTemplateVO createTemplate(NotificationTemplateSaveRequest request);

    NotificationTemplateVO updateTemplate(Long id, NotificationTemplateSaveRequest request);

    void deleteTemplate(Long id);

    PageResponse<NotificationDeliveryLogVO> pageDeliveryLogs(NotificationDeliveryLogQueryRequest request);

    void sendExamPublishNotifications(Long organizationId,
                                      Long examPlanId,
                                      String examName,
                                      LocalDateTime startTime,
                                      List<ExamCandidate> candidates);

    int dispatchUpcomingExamReminders();

    void sendScorePublishedNotification(ExamRecord record);

    void sendScoreAppealSubmittedNotification(ExamRecord record, ScoreAppeal appeal, List<SysUser> recipients);

    void sendScoreAppealResultNotification(ExamRecord record, String title, String content);

    void sendSecurityAlertNotification(LoginRiskLog log, String title, String content, List<SysUser> recipients);
}
