package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectexample.examsystem.common.PageResponse;
import com.projectexample.examsystem.dto.NotificationDeliveryLogQueryRequest;
import com.projectexample.examsystem.dto.NotificationTemplateSaveRequest;
import com.projectexample.examsystem.entity.ConfigItem;
import com.projectexample.examsystem.entity.ExamCandidate;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.entity.InAppMessage;
import com.projectexample.examsystem.entity.LoginRiskLog;
import com.projectexample.examsystem.entity.NotificationDeliveryLog;
import com.projectexample.examsystem.entity.NotificationTemplate;
import com.projectexample.examsystem.entity.ScoreAppeal;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.ConfigItemMapper;
import com.projectexample.examsystem.mapper.ExamCandidateMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.mapper.InAppMessageMapper;
import com.projectexample.examsystem.mapper.NotificationDeliveryLogMapper;
import com.projectexample.examsystem.mapper.NotificationTemplateMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.security.ExamPeriodProtectionService;
import com.projectexample.examsystem.service.NotificationService;
import com.projectexample.examsystem.vo.NotificationDeliveryLogVO;
import com.projectexample.examsystem.vo.NotificationTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String NOTICE_GROUP = "notice";
    private static final String CHANNEL_IN_APP = "IN_APP";
    private static final String CHANNEL_MOCK_SMS = "MOCK_SMS";
    private static final String KEY_IN_APP_ENABLED = "notice.message.enabled";
    private static final String KEY_MOCK_SMS_ENABLED = "notice.mock-sms.enabled";
    private static final String KEY_EXAM_REMINDER_ENABLED = "notice.exam-reminder.enabled";
    private static final String KEY_EXAM_REMINDER_LEAD_MINUTES = "notice.exam-reminder.lead-minutes";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{[^}]+}}");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationTemplateMapper notificationTemplateMapper;
    private final NotificationDeliveryLogMapper notificationDeliveryLogMapper;
    private final InAppMessageMapper inAppMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamCandidateMapper examCandidateMapper;
    private final ConfigItemMapper configItemMapper;
    private final AccessScopeService accessScopeService;
    private final ExamPeriodProtectionService examPeriodProtectionService;

    @Override
    public List<NotificationTemplateVO> listTemplates() {
        List<Long> visibleOrgIds = accessScopeService.visibleOrganizationIds();
        return notificationTemplateMapper.selectList(Wrappers.lambdaQuery(NotificationTemplate.class)
                        .and(!accessScopeService.isAdmin(), wrapper -> wrapper
                                .in(NotificationTemplate::getOrganizationId, visibleOrgIds.isEmpty() ? List.of(-1L) : visibleOrgIds)
                                .or()
                                .isNull(NotificationTemplate::getOrganizationId))
                        .orderByAsc(NotificationTemplate::getBusinessType, NotificationTemplate::getChannelType, NotificationTemplate::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public NotificationTemplateVO createTemplate(NotificationTemplateSaveRequest request) {
        examPeriodProtectionService.assertMutable("新增通知模板");
        NotificationTemplate entity = new NotificationTemplate();
        entity.setOrganizationId(accessScopeService.currentOrganizationId());
        ensureTemplateCodeUnique(null, entity.getOrganizationId(), request.getTemplateCode());
        apply(entity, request);
        notificationTemplateMapper.insert(entity);
        return toVO(requireTemplate(entity.getId()));
    }

    @Override
    public NotificationTemplateVO updateTemplate(Long id, NotificationTemplateSaveRequest request) {
        examPeriodProtectionService.assertMutable("更新通知模板");
        NotificationTemplate entity = requireTemplate(id);
        ensureTemplateCodeUnique(id, entity.getOrganizationId(), request.getTemplateCode());
        apply(entity, request);
        notificationTemplateMapper.updateById(entity);
        return toVO(requireTemplate(id));
    }

    @Override
    public void deleteTemplate(Long id) {
        examPeriodProtectionService.assertMutable("删除通知模板");
        requireTemplate(id);
        notificationTemplateMapper.deleteById(id);
    }

    @Override
    public PageResponse<NotificationDeliveryLogVO> pageDeliveryLogs(NotificationDeliveryLogQueryRequest request) {
        List<Long> accessibleOrgIds = accessScopeService.accessibleOrganizationIds();
        Page<NotificationDeliveryLog> page = notificationDeliveryLogMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                Wrappers.lambdaQuery(NotificationDeliveryLog.class)
                        .in(!accessScopeService.isAdmin(), NotificationDeliveryLog::getOrganizationId, accessibleOrgIds.isEmpty() ? List.of(-1L) : accessibleOrgIds)
                        .eq(StringUtils.hasText(request.getBusinessType()), NotificationDeliveryLog::getBusinessType, normalizeUpper(request.getBusinessType()))
                        .eq(StringUtils.hasText(request.getChannelType()), NotificationDeliveryLog::getChannelType, normalizeUpper(request.getChannelType()))
                        .eq(StringUtils.hasText(request.getDeliveryStatus()), NotificationDeliveryLog::getDeliveryStatus, normalizeUpper(request.getDeliveryStatus()))
                        .eq(request.getRecipientUserId() != null, NotificationDeliveryLog::getRecipientUserId, request.getRecipientUserId())
                        .orderByDesc(NotificationDeliveryLog::getSentAt, NotificationDeliveryLog::getId)
        );
        return PageResponse.of(page, this::toVO);
    }

    @Override
    public void sendExamPublishNotifications(Long organizationId,
                                             Long examPlanId,
                                             String examName,
                                             LocalDateTime startTime,
                                             List<ExamCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = loadUsers(candidates.stream().map(ExamCandidate::getUserId).toList());
        for (ExamCandidate candidate : candidates) {
            SysUser recipient = userMap.get(candidate.getUserId());
            if (recipient == null) {
                continue;
            }
            Map<String, String> variables = baseExamVariables(examName, startTime, candidate.getCandidateName(), candidate.getAccessCode());
            dispatchWithTemplates(
                    organizationId,
                    "EXAM_PUBLISH",
                    "EXAM_PLAN",
                    examPlanId,
                    recipient,
                    "EXAM_PUBLISH:%s:%s".formatted(examPlanId, recipient.getId()),
                    variables
            );
        }
    }

    @Override
    public int dispatchUpcomingExamReminders() {
        if (!configBoolean(KEY_EXAM_REMINDER_ENABLED, true)) {
            return 0;
        }
        int leadMinutes = configInt(KEY_EXAM_REMINDER_LEAD_MINUTES, 30);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(leadMinutes);
        List<ExamPlan> plans = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                .eq(ExamPlan::getPublishStatus, 1)
                .eq(ExamPlan::getStatus, 1)
                .ge(ExamPlan::getStartTime, now)
                .le(ExamPlan::getStartTime, deadline)
                .orderByAsc(ExamPlan::getStartTime, ExamPlan::getId));
        int deliveredCount = 0;
        for (ExamPlan plan : plans) {
            List<ExamCandidate> candidates = examCandidateMapper.selectList(Wrappers.lambdaQuery(ExamCandidate.class)
                    .eq(ExamCandidate::getExamPlanId, plan.getId())
                    .orderByAsc(ExamCandidate::getId));
            if (candidates.isEmpty()) {
                continue;
            }
            Map<Long, SysUser> userMap = loadUsers(candidates.stream().map(ExamCandidate::getUserId).toList());
            for (ExamCandidate candidate : candidates) {
                SysUser recipient = userMap.get(candidate.getUserId());
                if (recipient == null) {
                    continue;
                }
                Map<String, String> variables = baseExamVariables(plan.getExamName(), plan.getStartTime(), candidate.getCandidateName(), candidate.getAccessCode());
                variables.put("leadMinutes", String.valueOf(leadMinutes));
                String businessKeyPrefix = "EXAM_REMINDER:%s:%s:%s".formatted(
                        plan.getId(),
                        recipient.getId(),
                        plan.getStartTime() == null ? "UNKNOWN" : plan.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                deliveredCount += dispatchWithTemplates(plan.getOrganizationId(), "EXAM_REMINDER", "EXAM_PLAN", plan.getId(), recipient, businessKeyPrefix, variables);
            }
        }
        return deliveredCount;
    }

    @Override
    public void sendScorePublishedNotification(ExamRecord record) {
        if (record == null) {
            return;
        }
        SysUser recipient = sysUserMapper.selectById(record.getUserId());
        if (recipient == null) {
            return;
        }
        dispatchWithTemplates(
                resolveOrganizationId(recipient),
                "SCORE_PUBLISH",
                "SCORE_RECORD",
                record.getId(),
                recipient,
                "SCORE_PUBLISH:%s:%s".formatted(record.getId(), recipient.getId()),
                Map.of(
                        "examName", defaultText(record.getExamName()),
                        "candidateName", defaultText(record.getCandidateName())
                )
        );
    }

    @Override
    public void sendScoreAppealSubmittedNotification(ExamRecord record, ScoreAppeal appeal, List<SysUser> recipients) {
        if (record == null || appeal == null || recipients == null || recipients.isEmpty()) {
            return;
        }
        for (SysUser recipient : distinctUsers(recipients)) {
            dispatchWithTemplates(
                    resolveOrganizationId(recipient),
                    "SCORE_APPEAL",
                    "SCORE_APPEAL",
                    appeal.getId(),
                    recipient,
                    "SCORE_APPEAL:%s:%s".formatted(appeal.getId(), recipient.getId()),
                    Map.of(
                            "examName", defaultText(record.getExamName()),
                            "candidateName", defaultText(record.getCandidateName())
                    )
            );
        }
    }

    @Override
    public void sendScoreAppealResultNotification(ExamRecord record, String title, String content) {
        if (record == null) {
            return;
        }
        SysUser recipient = sysUserMapper.selectById(record.getUserId());
        if (recipient == null) {
            return;
        }
        dispatchWithTemplates(
                resolveOrganizationId(recipient),
                "SCORE_APPEAL_RESULT",
                "SCORE_RECORD",
                record.getId(),
                recipient,
                "SCORE_APPEAL_RESULT:%s:%s:%s".formatted(record.getId(), recipient.getId(), normalizeBusinessKeySuffix(title)),
                Map.of(
                        "title", defaultText(title),
                        "content", defaultText(content),
                        "examName", defaultText(record.getExamName()),
                        "candidateName", defaultText(record.getCandidateName())
                )
        );
    }

    @Override
    public void sendSecurityAlertNotification(LoginRiskLog log, String title, String content, List<SysUser> recipients) {
        if (log == null || recipients == null || recipients.isEmpty()) {
            return;
        }
        for (SysUser recipient : distinctUsers(recipients)) {
            dispatchWithTemplates(
                    resolveOrganizationId(recipient),
                    "SECURITY_ALERT",
                    "LOGIN_RISK",
                    log.getId(),
                    recipient,
                    "SECURITY_ALERT:%s:%s".formatted(log.getId(), recipient.getId()),
                    Map.of(
                            "title", defaultText(title),
                            "content", defaultText(content),
                            "username", defaultText(log.getUsername()),
                            "riskLevel", defaultText(log.getRiskLevel()),
                            "loginAt", formatTime(log.getLoginAt())
                    )
            );
        }
    }

    private int dispatchWithTemplates(Long organizationId,
                                      String businessType,
                                      String relatedType,
                                      Long relatedId,
                                      SysUser recipient,
                                      String businessKeyPrefix,
                                      Map<String, String> variables) {
        List<NotificationTemplate> templates = resolveTemplates(organizationId, businessType);
        int deliveredCount = 0;
        for (NotificationTemplate template : templates) {
            String businessKey = businessKeyPrefix + ":" + template.getChannelType();
            if (hasDelivered(businessKey)) {
                continue;
            }
            if (CHANNEL_IN_APP.equalsIgnoreCase(template.getChannelType())) {
                if (!configBoolean(KEY_IN_APP_ENABLED, true)) {
                    continue;
                }
                deliveredCount += deliverInApp(template, organizationId, relatedType, relatedId, recipient, businessKey, variables);
                continue;
            }
            if (CHANNEL_MOCK_SMS.equalsIgnoreCase(template.getChannelType())) {
                if (!configBoolean(KEY_MOCK_SMS_ENABLED, true)) {
                    continue;
                }
                deliveredCount += deliverMockSms(template, organizationId, relatedType, relatedId, recipient, businessKey, variables);
            }
        }
        return deliveredCount;
    }

    private List<NotificationTemplate> resolveTemplates(Long organizationId, String businessType) {
        List<NotificationTemplate> candidates = notificationTemplateMapper.selectList(Wrappers.lambdaQuery(NotificationTemplate.class)
                .eq(NotificationTemplate::getBusinessType, normalizeUpper(businessType))
                .eq(NotificationTemplate::getStatus, 1)
                .and(wrapper -> wrapper
                        .eq(NotificationTemplate::getOrganizationId, organizationId)
                        .or()
                        .isNull(NotificationTemplate::getOrganizationId))
                .orderByAsc(NotificationTemplate::getChannelType, NotificationTemplate::getId));
        Map<String, NotificationTemplate> selected = new LinkedHashMap<>();
        for (NotificationTemplate candidate : candidates.stream()
                .sorted(Comparator.comparing((NotificationTemplate item) -> !sameOrganization(item.getOrganizationId(), organizationId))
                        .thenComparing(NotificationTemplate::getId))
                .toList()) {
            selected.putIfAbsent(candidate.getChannelType(), candidate);
        }
        return selected.values().stream().toList();
    }

    private int deliverInApp(NotificationTemplate template,
                             Long organizationId,
                             String relatedType,
                             Long relatedId,
                             SysUser recipient,
                             String businessKey,
                             Map<String, String> variables) {
        String title = render(template.getTitleTemplate(), variables);
        String content = render(template.getContentTemplate(), variables);

        InAppMessage message = new InAppMessage();
        message.setRecipientUserId(recipient.getId());
        message.setTitle(title);
        message.setMessageType(template.getBusinessType());
        message.setContent(content);
        message.setRelatedType(relatedType);
        message.setRelatedId(relatedId);
        message.setReadFlag(0);
        inAppMessageMapper.insert(message);

        NotificationDeliveryLog log = buildDeliveryLog(template, organizationId, recipient, businessKey, relatedType, relatedId, title, content);
        log.setDeliveryStatus("DELIVERED");
        log.setProviderTrace("IN_APP_MESSAGE_ID=" + message.getId());
        notificationDeliveryLogMapper.insert(log);
        return 1;
    }

    private int deliverMockSms(NotificationTemplate template,
                               Long organizationId,
                               String relatedType,
                               Long relatedId,
                               SysUser recipient,
                               String businessKey,
                               Map<String, String> variables) {
        String title = render(template.getTitleTemplate(), variables);
        String content = render(template.getContentTemplate(), variables);
        NotificationDeliveryLog log = buildDeliveryLog(template, organizationId, recipient, businessKey, relatedType, relatedId, title, content);
        if (!StringUtils.hasText(recipient.getPhone())) {
            log.setDeliveryStatus("SKIPPED");
            log.setProviderTrace("MOCK_SMS_SKIP: recipient phone is empty");
            notificationDeliveryLogMapper.insert(log);
            return 0;
        }
        log.setRecipientTarget(recipient.getPhone());
        log.setDeliveryStatus("DELIVERED");
        log.setProviderTrace("MOCK_SMS -> " + recipient.getPhone());
        notificationDeliveryLogMapper.insert(log);
        return 1;
    }

    private NotificationDeliveryLog buildDeliveryLog(NotificationTemplate template,
                                                     Long organizationId,
                                                     SysUser recipient,
                                                     String businessKey,
                                                     String relatedType,
                                                     Long relatedId,
                                                     String title,
                                                     String content) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setOrganizationId(organizationId);
        log.setBusinessType(template.getBusinessType());
        log.setChannelType(template.getChannelType());
        log.setTemplateCode(template.getTemplateCode());
        log.setRecipientUserId(recipient.getId());
        log.setRecipientName(resolveUserName(recipient));
        log.setRecipientTarget(CHANNEL_MOCK_SMS.equalsIgnoreCase(template.getChannelType()) ? recipient.getPhone() : recipient.getUsername());
        log.setTitle(title);
        log.setContent(content);
        log.setRelatedType(relatedType);
        log.setRelatedId(relatedId);
        log.setBusinessKey(businessKey);
        log.setSentAt(LocalDateTime.now());
        return log;
    }

    private NotificationTemplate requireTemplate(Long id) {
        NotificationTemplate template = notificationTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(4040, "Notification template not found");
        }
        if (!accessScopeService.isAdmin()) {
            if (template.getOrganizationId() == null) {
                throw new BusinessException(4031, "Current user cannot access the target organization");
            }
            accessScopeService.assertOrganizationAccessible(template.getOrganizationId());
        }
        return template;
    }

    private void ensureTemplateCodeUnique(Long currentId, Long organizationId, String templateCode) {
        NotificationTemplate existing = notificationTemplateMapper.selectOne(Wrappers.lambdaQuery(NotificationTemplate.class)
                .eq(NotificationTemplate::getOrganizationId, organizationId)
                .eq(NotificationTemplate::getTemplateCode, normalizeUpper(templateCode))
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(4005, "通知模板编码已存在");
        }
    }

    private boolean hasDelivered(String businessKey) {
        Long count = notificationDeliveryLogMapper.selectCount(Wrappers.lambdaQuery(NotificationDeliveryLog.class)
                .eq(NotificationDeliveryLog::getBusinessKey, businessKey)
                .eq(NotificationDeliveryLog::getDeliveryStatus, "DELIVERED"));
        return count != null && count > 0;
    }

    private Map<Long, SysUser> loadUsers(List<Long> userIds) {
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> right));
    }

    private LinkedHashSet<SysUser> distinctUsers(List<SysUser> recipients) {
        return recipients.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, String> baseExamVariables(String examName,
                                                  LocalDateTime startTime,
                                                  String candidateName,
                                                  String accessCode) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("examName", defaultText(examName));
        variables.put("startTime", formatTime(startTime));
        variables.put("candidateName", defaultText(candidateName));
        variables.put("accessCode", defaultText(accessCode));
        return variables;
    }

    private void apply(NotificationTemplate entity, NotificationTemplateSaveRequest request) {
        entity.setTemplateCode(normalizeUpper(request.getTemplateCode()));
        entity.setTemplateName(trimValue(request.getTemplateName(), 128));
        entity.setBusinessType(normalizeUpper(request.getBusinessType()));
        entity.setChannelType(normalizeUpper(request.getChannelType()));
        entity.setTitleTemplate(trimValue(request.getTitleTemplate(), 255));
        entity.setContentTemplate(trimValue(request.getContentTemplate(), 2000));
        entity.setStatus(request.getStatus());
    }

    private NotificationTemplateVO toVO(NotificationTemplate entity) {
        return NotificationTemplateVO.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .businessType(entity.getBusinessType())
                .channelType(entity.getChannelType())
                .titleTemplate(entity.getTitleTemplate())
                .contentTemplate(entity.getContentTemplate())
                .status(entity.getStatus())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private NotificationDeliveryLogVO toVO(NotificationDeliveryLog entity) {
        return NotificationDeliveryLogVO.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .businessType(entity.getBusinessType())
                .channelType(entity.getChannelType())
                .templateCode(entity.getTemplateCode())
                .recipientUserId(entity.getRecipientUserId())
                .recipientName(entity.getRecipientName())
                .recipientTarget(entity.getRecipientTarget())
                .title(entity.getTitle())
                .content(entity.getContent())
                .relatedType(entity.getRelatedType())
                .relatedId(entity.getRelatedId())
                .businessKey(entity.getBusinessKey())
                .deliveryStatus(entity.getDeliveryStatus())
                .providerTrace(entity.getProviderTrace())
                .sentAt(entity.getSentAt())
                .build();
    }

    private String render(String template, Map<String, String> variables) {
        String rendered = defaultText(template);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", defaultText(entry.getValue()));
        }
        return TOKEN_PATTERN.matcher(rendered).replaceAll("");
    }

    private Long resolveOrganizationId(SysUser user) {
        return user == null ? null : user.getOrganizationId();
    }

    private String resolveUserName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getNickname();
    }

    private String trimValue(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(TIME_FORMATTER);
    }

    private String normalizeUpper(String value) {
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeBusinessKeySuffix(String value) {
        String normalized = defaultText(value).replaceAll("[^A-Za-z0-9]+", "_");
        return StringUtils.hasText(normalized) ? normalized : "DEFAULT";
    }

    private boolean sameOrganization(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private boolean configBoolean(String key, boolean defaultValue) {
        String raw = configValue(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    private int configInt(String key, int defaultValue) {
        String raw = configValue(key);
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String configValue(String key) {
        ConfigItem item = configItemMapper.selectOne(Wrappers.lambdaQuery(ConfigItem.class)
                .eq(ConfigItem::getConfigKey, key)
                .eq(ConfigItem::getConfigGroup, NOTICE_GROUP)
                .eq(ConfigItem::getStatus, 1)
                .last("limit 1"));
        if (item != null) {
            return item.getConfigValue();
        }
        ConfigItem fallback = configItemMapper.selectOne(Wrappers.lambdaQuery(ConfigItem.class)
                .eq(ConfigItem::getConfigKey, key)
                .eq(ConfigItem::getStatus, 1)
                .last("limit 1"));
        return fallback == null ? null : fallback.getConfigValue();
    }
}
