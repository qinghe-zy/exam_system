package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.LoginRequest;
import com.projectexample.examsystem.dto.PasswordResetRequest;
import com.projectexample.examsystem.dto.RegisterRequest;
import com.projectexample.examsystem.dto.SendVerificationCodeRequest;
import com.projectexample.examsystem.entity.ConfigItem;
import com.projectexample.examsystem.entity.LoginRiskLog;
import com.projectexample.examsystem.entity.Organization;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.entity.VerificationCode;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.ConfigItemMapper;
import com.projectexample.examsystem.mapper.LoginRiskLogMapper;
import com.projectexample.examsystem.mapper.OrganizationMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.mapper.VerificationCodeMapper;
import com.projectexample.examsystem.security.JwtTokenProvider;
import com.projectexample.examsystem.security.RolePermissionCatalog;
import com.projectexample.examsystem.service.AuthService;
import com.projectexample.examsystem.service.NotificationService;
import com.projectexample.examsystem.service.SysUserService;
import com.projectexample.examsystem.vo.AuthRegisterOptionVO;
import com.projectexample.examsystem.vo.AuthTokenVO;
import com.projectexample.examsystem.vo.CurrentUserVO;
import com.projectexample.examsystem.vo.VerificationCodeSendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_SMS = "SMS";
    private static final String MESSAGE_TYPE_SECURITY_ALERT = "SECURITY_ALERT";
    private static final String SECURITY_GROUP = "auth_security";
    private static final String KEY_LOGIN_MAX_FAILURES = "auth.security.login.max-failures";
    private static final String KEY_LOGIN_LOCK_MINUTES = "auth.security.login.lock-minutes";
    private static final String KEY_LOGIN_IP_WINDOW_SECONDS = "auth.security.login.ip-rate-limit.window-seconds";
    private static final String KEY_LOGIN_IP_MAX_ATTEMPTS = "auth.security.login.ip-rate-limit.max-attempts";
    private static final String KEY_VERIFY_COOLDOWN_SECONDS = "auth.security.verification.cooldown-seconds";
    private static final String KEY_VERIFY_WINDOW_MINUTES = "auth.security.verification.window-minutes";
    private static final String KEY_VERIFY_MAX_SENDS = "auth.security.verification.max-sends-per-window";
    private static final String KEY_ALERT_ENABLED = "auth.security.alert.message.enabled";
    private static final String KEY_ALERT_RECIPIENT_ROLES = "auth.security.alert.recipient.roles";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;
    private final OrganizationMapper organizationMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final LoginRiskLogMapper loginRiskLogMapper;
    private final ConfigItemMapper configItemMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RolePermissionCatalog rolePermissionCatalog;
    private final NotificationService notificationService;

    @Value("${app.auth.verification.expire-minutes:10}")
    private long verificationExpireMinutes;

    @Value("${app.auth.verification.mock-enabled:true}")
    private boolean verificationMockEnabled;

    @Override
    public AuthTokenVO login(LoginRequest request, String clientIp, String userAgent, String deviceFingerprint, String deviceInfo) {
        String sanitizedIp = trimValue(clientIp, 64);
        ensureIpRateLimitAllowed(request.getUsername(), sanitizedIp, userAgent, deviceFingerprint, deviceInfo);

        SysUser user = sysUserService.findByUsername(request.getUsername());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            recordLoginRisk(request.getUsername(), null, 0, sanitizedIp, userAgent, deviceFingerprint, deviceInfo, "MEDIUM", "账号不存在或已停用");
            throw new BusinessException("Invalid username or password");
        }

        ensureAccountUnlocked(user, sanitizedIp, userAgent, deviceFingerprint, deviceInfo);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handlePasswordFailure(request.getUsername(), user, sanitizedIp, userAgent, deviceFingerprint, deviceInfo);
            throw new BusinessException("Invalid username or password");
        }

        clearLoginFailureState(user);
        user.setSessionVersion((user.getSessionVersion() == null ? 0 : user.getSessionVersion()) + 1);
        persistLoginSecurityState(user);
        String riskLevel = successRiskLevel(user, sanitizedIp, deviceFingerprint);
        String riskReason = successRiskReason(user, sanitizedIp, deviceFingerprint);
        recordLoginRisk(request.getUsername(), user, 1, sanitizedIp, userAgent, deviceFingerprint, deviceInfo, riskLevel, riskReason);
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getNickname(), user.getRoleCode(), user.getSessionVersion());
        return new AuthTokenVO(token, toCurrentUser(user));
    }

    @Override
    public CurrentUserVO currentUser(String username) {
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(4010, "User session is invalid");
        }
        return toCurrentUser(user);
    }

    @Override
    public List<AuthRegisterOptionVO> listRegisterOptions() {
        return organizationMapper.selectList(Wrappers.lambdaQuery(Organization.class)
                        .eq(Organization::getStatus, 1)
                        .in(Organization::getOrgType, List.of("CLASS", "DEPARTMENT")))
                .stream()
                .map(item -> AuthRegisterOptionVO.builder()
                        .organizationId(item.getId())
                        .organizationName(item.getOrgName())
                        .organizationType(item.getOrgType())
                        .build())
                .toList();
    }

    @Override
    public VerificationCodeSendVO sendVerificationCode(SendVerificationCodeRequest request) {
        String purpose = normalizePurpose(request.getPurpose());
        String channel = normalizeChannel(request.getChannel());
        String targetValue = sanitizeTarget(request.getTargetValue());

        if (PURPOSE_RESET_PASSWORD.equals(purpose)) {
            SysUser user = requireUser(request.getUsername());
            targetValue = resolveResetTarget(channel, user);
        }
        if (!StringUtils.hasText(targetValue)) {
            throw new BusinessException(4004, "验证码接收目标不能为空");
        }
        ensureVerificationCodeSendAllowed(purpose, channel, targetValue);

        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(verificationExpireMinutes);
        verificationCodeMapper.update(null, Wrappers.lambdaUpdate(VerificationCode.class)
                .eq(VerificationCode::getPurpose, purpose)
                .eq(VerificationCode::getChannel, channel)
                .eq(VerificationCode::getTargetValue, targetValue)
                .set(VerificationCode::getConsumedFlag, 1));

        VerificationCode entity = new VerificationCode();
        entity.setPurpose(purpose);
        entity.setChannel(channel);
        entity.setTargetValue(targetValue);
        entity.setVerifyCode(code);
        entity.setUsername(request.getUsername());
        entity.setOrganizationId(request.getOrganizationId());
        entity.setDeliveryTrace(buildDeliveryTrace(channel, targetValue, code));
        entity.setExpiresAt(expiresAt);
        entity.setVerifiedFlag(0);
        entity.setConsumedFlag(0);
        verificationCodeMapper.insert(entity);

        return VerificationCodeSendVO.builder()
                .purpose(purpose)
                .channel(channel)
                .targetValue(targetValue)
                .expiresAt(expiresAt)
                .deliveryTrace(entity.getDeliveryTrace())
                .mockCode(verificationMockEnabled ? code : null)
                .build();
    }

    @Override
    public void register(RegisterRequest request) {
        if (sysUserService.findByUsername(request.getUsername()) != null) {
            throw new BusinessException(4009, "用户名已存在");
        }
        String channel = normalizeChannel(request.getVerificationChannel());
        String targetValue = resolveRegisterTarget(channel, request);
        VerificationCode verificationCode = requireValidCode(PURPOSE_REGISTER, channel, targetValue, request.getVerificationCode());
        Organization organization = requireOrganization(request.getOrganizationId());

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getFullName());
        user.setFullName(request.getFullName());
        user.setRoleCode("STUDENT");
        user.setOrganizationId(organization.getId());
        user.setOrganizationName(organization.getOrgName());
        user.setDepartmentName(StringUtils.hasText(request.getDepartmentName()) ? request.getDepartmentName() : organization.getOrgName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCandidateNo(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setSessionVersion(0);
        user.setLoginFailCount(0);
        user.setLastLoginFailureAt(null);
        user.setLockUntil(null);
        user.setStatus(1);
        sysUserMapper.insert(user);
        markVerificationCodeConsumed(verificationCode);
    }

    @Override
    public void resetPassword(PasswordResetRequest request) {
        SysUser user = requireUser(request.getUsername());
        String channel = normalizeChannel(request.getVerificationChannel());
        String targetValue = resolveResetTarget(channel, user);
        VerificationCode verificationCode = requireValidCode(PURPOSE_RESET_PASSWORD, channel, targetValue, request.getVerificationCode());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        clearLoginFailureState(user);
        user.setSessionVersion((user.getSessionVersion() == null ? 0 : user.getSessionVersion()) + 1);
        persistPasswordAndLoginSecurityState(user);
        markVerificationCodeConsumed(verificationCode);
    }

    @Override
    public void logout(String username) {
        SysUser user = requireUser(username);
        user.setSessionVersion((user.getSessionVersion() == null ? 0 : user.getSessionVersion()) + 1);
        persistLoginSecurityState(user);
    }

    private CurrentUserVO toCurrentUser(SysUser user) {
        return CurrentUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .fullName(user.getFullName())
                .roleCode(user.getRoleCode())
                .organizationName(user.getOrganizationName())
                .permissions(rolePermissionCatalog.permissionsForRole(user.getRoleCode()))
                .build();
    }

    private SysUser requireUser(String username) {
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(4040, "用户不存在");
        }
        return user;
    }

    private Organization requireOrganization(Long organizationId) {
        Organization organization = organizationMapper.selectById(organizationId);
        if (organization == null || organization.getStatus() == null || organization.getStatus() != 1) {
            throw new BusinessException(4040, "注册组织不存在");
        }
        return organization;
    }

    private void ensureVerificationCodeSendAllowed(String purpose, String channel, String targetValue) {
        int cooldownSeconds = configInt(KEY_VERIFY_COOLDOWN_SECONDS, 60);
        VerificationCode latest = verificationCodeMapper.selectOne(Wrappers.lambdaQuery(VerificationCode.class)
                .eq(VerificationCode::getPurpose, purpose)
                .eq(VerificationCode::getChannel, channel)
                .eq(VerificationCode::getTargetValue, targetValue)
                .orderByDesc(VerificationCode::getCreateTime, VerificationCode::getId)
                .last("limit 1"));
        if (latest != null && latest.getCreateTime() != null && latest.getCreateTime().isAfter(LocalDateTime.now().minusSeconds(cooldownSeconds))) {
            throw new BusinessException(4014, "验证码发送过于频繁，请稍后再试");
        }

        int maxSends = configInt(KEY_VERIFY_MAX_SENDS, 5);
        int windowMinutes = configInt(KEY_VERIFY_WINDOW_MINUTES, 60);
        long recentSendCount = verificationCodeMapper.selectCount(Wrappers.lambdaQuery(VerificationCode.class)
                .eq(VerificationCode::getPurpose, purpose)
                .eq(VerificationCode::getChannel, channel)
                .eq(VerificationCode::getTargetValue, targetValue)
                .ge(VerificationCode::getCreateTime, LocalDateTime.now().minusMinutes(windowMinutes)));
        if (recentSendCount >= maxSends) {
            throw new BusinessException(4014, "验证码发送次数已达到窗口上限，请稍后再试");
        }
    }

    private String resolveRegisterTarget(String channel, RegisterRequest request) {
        return switch (channel) {
            case CHANNEL_EMAIL -> {
                if (!StringUtils.hasText(request.getEmail())) {
                    throw new BusinessException(4004, "注册时必须提供邮箱");
                }
                yield sanitizeTarget(request.getEmail());
            }
            case CHANNEL_SMS -> {
                if (!StringUtils.hasText(request.getPhone())) {
                    throw new BusinessException(4004, "注册时必须提供手机号");
                }
                yield sanitizeTarget(request.getPhone());
            }
            default -> throw new BusinessException(4004, "不支持的验证码通道");
        };
    }

    private String resolveResetTarget(String channel, SysUser user) {
        return switch (channel) {
            case CHANNEL_EMAIL -> {
                if (!StringUtils.hasText(user.getEmail())) {
                    throw new BusinessException(4004, "当前账号未绑定邮箱，无法使用邮箱找回");
                }
                yield sanitizeTarget(user.getEmail());
            }
            case CHANNEL_SMS -> {
                if (!StringUtils.hasText(user.getPhone())) {
                    throw new BusinessException(4004, "当前账号未绑定手机号，无法使用短信找回");
                }
                yield sanitizeTarget(user.getPhone());
            }
            default -> throw new BusinessException(4004, "不支持的验证码通道");
        };
    }

    private VerificationCode requireValidCode(String purpose, String channel, String targetValue, String code) {
        VerificationCode verificationCode = verificationCodeMapper.selectOne(Wrappers.lambdaQuery(VerificationCode.class)
                .eq(VerificationCode::getPurpose, purpose)
                .eq(VerificationCode::getChannel, channel)
                .eq(VerificationCode::getTargetValue, targetValue)
                .eq(VerificationCode::getVerifyCode, code)
                .eq(VerificationCode::getConsumedFlag, 0)
                .orderByDesc(VerificationCode::getId)
                .last("limit 1"));
        if (verificationCode == null) {
            throw new BusinessException(4004, "验证码无效，请重新获取");
        }
        if (verificationCode.getExpiresAt() == null || verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(4004, "验证码已过期，请重新获取");
        }
        return verificationCode;
    }

    private void markVerificationCodeConsumed(VerificationCode verificationCode) {
        verificationCode.setConsumedFlag(1);
        verificationCode.setVerifiedFlag(1);
        verificationCodeMapper.updateById(verificationCode);
    }

    private void ensureIpRateLimitAllowed(String username,
                                          String clientIp,
                                          String userAgent,
                                          String deviceFingerprint,
                                          String deviceInfo) {
        if (!StringUtils.hasText(clientIp)) {
            return;
        }
        int windowSeconds = configInt(KEY_LOGIN_IP_WINDOW_SECONDS, 300);
        int maxAttempts = configInt(KEY_LOGIN_IP_MAX_ATTEMPTS, 12);
        long recentAttempts = loginRiskLogMapper.selectCount(Wrappers.lambdaQuery(LoginRiskLog.class)
                .eq(LoginRiskLog::getClientIp, clientIp)
                .eq(LoginRiskLog::getSuccessFlag, 0)
                .ge(LoginRiskLog::getLoginAt, LocalDateTime.now().minusSeconds(windowSeconds)));
        if (recentAttempts < maxAttempts) {
            return;
        }
        String reason = "同一 IP 在 " + windowSeconds + " 秒内失败登录次数达到 " + maxAttempts + " 次，已触发限流拦截";
        LoginRiskLog log = recordLoginRisk(username, null, 0, clientIp, userAgent, deviceFingerprint, deviceInfo, "HIGH", reason);
        if (recentAttempts == maxAttempts) {
            publishSecurityAlert(log, "登录 IP 触发限流", reason);
        }
        throw new BusinessException(4012, "当前 IP 登录过于频繁，请稍后再试");
    }

    private void ensureAccountUnlocked(SysUser user,
                                       String clientIp,
                                       String userAgent,
                                       String deviceFingerprint,
                                       String deviceInfo) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(now)) {
            String message = "当前账号因多次登录失败已被临时锁定，请于 " + TIME_FORMATTER.format(user.getLockUntil()) + " 后重试";
            recordLoginRisk(user.getUsername(), user, 0, clientIp, userAgent, deviceFingerprint, deviceInfo, "HIGH",
                    "账号处于临时锁定期，锁定截止时间：" + TIME_FORMATTER.format(user.getLockUntil()));
            throw new BusinessException(4013, message);
        }
        if (user.getLockUntil() != null && !user.getLockUntil().isAfter(now)) {
            clearLoginFailureState(user);
            persistLoginSecurityState(user);
        }
    }

    private void handlePasswordFailure(String username,
                                       SysUser user,
                                       String clientIp,
                                       String userAgent,
                                       String deviceFingerprint,
                                       String deviceInfo) {
        LocalDateTime now = LocalDateTime.now();
        int lockMinutes = configInt(KEY_LOGIN_LOCK_MINUTES, 30);
        int maxFailures = configInt(KEY_LOGIN_MAX_FAILURES, 5);
        int nextFailureCount = user.getLastLoginFailureAt() == null || user.getLastLoginFailureAt().isBefore(now.minusMinutes(lockMinutes))
                ? 1
                : (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        user.setLoginFailCount(nextFailureCount);
        user.setLastLoginFailureAt(now);
        boolean locked = nextFailureCount >= maxFailures;
        user.setLockUntil(locked ? now.plusMinutes(lockMinutes) : null);
        persistLoginSecurityState(user);

        String riskLevel = locked ? "HIGH" : nextFailureCount >= Math.max(2, maxFailures - 1) ? "MEDIUM" : "LOW";
        String riskReason;
        if (locked) {
            riskReason = "账号在 " + lockMinutes + " 分钟内连续失败 " + nextFailureCount + " 次，已临时锁定 " + lockMinutes + " 分钟";
        } else if (nextFailureCount >= 2) {
            riskReason = "账号连续失败登录 " + nextFailureCount + " 次，已接近锁定阈值";
        } else {
            riskReason = "登录失败，已记录基础风险信息";
        }

        LoginRiskLog log = recordLoginRisk(username, user, 0, clientIp, userAgent, deviceFingerprint, deviceInfo, riskLevel, riskReason);
        if (locked) {
            publishSecurityAlert(log, "账号触发临时锁定", riskReason);
        }
    }

    private void clearLoginFailureState(SysUser user) {
        user.setLoginFailCount(0);
        user.setLastLoginFailureAt(null);
        user.setLockUntil(null);
    }

    private void persistLoginSecurityState(SysUser user) {
        sysUserMapper.update(null, Wrappers.lambdaUpdate(SysUser.class)
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getSessionVersion, user.getSessionVersion() == null ? 0 : user.getSessionVersion())
                .set(SysUser::getLoginFailCount, user.getLoginFailCount() == null ? 0 : user.getLoginFailCount())
                .set(SysUser::getLastLoginFailureAt, user.getLastLoginFailureAt())
                .set(SysUser::getLockUntil, user.getLockUntil()));
    }

    private void persistPasswordAndLoginSecurityState(SysUser user) {
        sysUserMapper.update(null, Wrappers.lambdaUpdate(SysUser.class)
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, user.getPassword())
                .set(SysUser::getSessionVersion, user.getSessionVersion() == null ? 0 : user.getSessionVersion())
                .set(SysUser::getLoginFailCount, user.getLoginFailCount() == null ? 0 : user.getLoginFailCount())
                .set(SysUser::getLastLoginFailureAt, user.getLastLoginFailureAt())
                .set(SysUser::getLockUntil, user.getLockUntil()));
    }

    private String normalizePurpose(String purpose) {
        String normalized = String.valueOf(purpose).trim().toUpperCase(Locale.ROOT);
        if (!List.of(PURPOSE_REGISTER, PURPOSE_RESET_PASSWORD).contains(normalized)) {
            throw new BusinessException(4004, "不支持的验证码用途");
        }
        return normalized;
    }

    private String normalizeChannel(String channel) {
        String normalized = String.valueOf(channel).trim().toUpperCase(Locale.ROOT);
        if (!List.of(CHANNEL_EMAIL, CHANNEL_SMS).contains(normalized)) {
            throw new BusinessException(4004, "不支持的验证码通道");
        }
        return normalized;
    }

    private String sanitizeTarget(String targetValue) {
        return StringUtils.hasText(targetValue) ? targetValue.trim() : "";
    }

    private String buildDeliveryTrace(String channel, String targetValue, String code) {
        String prefix = CHANNEL_SMS.equals(channel) ? "MOCK_SMS" : "MOCK_EMAIL";
        return prefix + " -> " + targetValue + "，验证码：" + code;
    }

    private LoginRiskLog recordLoginRisk(String username,
                                         SysUser user,
                                         int successFlag,
                                         String clientIp,
                                         String userAgent,
                                         String deviceFingerprint,
                                         String deviceInfo,
                                         String riskLevel,
                                         String riskReason) {
        LoginRiskLog log = new LoginRiskLog();
        log.setUsername(username);
        log.setUserId(user == null ? null : user.getId());
        log.setRoleCode(user == null ? null : user.getRoleCode());
        log.setSuccessFlag(successFlag);
        log.setClientIp(trimValue(clientIp, 64));
        log.setUserAgent(trimValue(userAgent, 500));
        log.setDeviceFingerprint(trimValue(deviceFingerprint, 255));
        log.setDeviceInfo(trimValue(deviceInfo, 1000));
        log.setRiskLevel(riskLevel);
        log.setRiskReason(trimValue(riskReason, 500));
        log.setLoginAt(LocalDateTime.now());
        loginRiskLogMapper.insert(log);
        return log;
    }

    private void publishSecurityAlert(LoginRiskLog log, String title, String detail) {
        if (!configBoolean(KEY_ALERT_ENABLED, true)) {
            return;
        }
        List<String> roleCodes = parseRoleCodes(configValue(KEY_ALERT_RECIPIENT_ROLES), "ADMIN,ORG_ADMIN");
        if (roleCodes.isEmpty()) {
            return;
        }
        List<SysUser> recipients = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
                .in(SysUser::getRoleCode, roleCodes)
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.sendSecurityAlertNotification(
                log,
                "登录安全告警",
                title + "：账号=" + log.getUsername()
                        + "，IP=" + defaultText(log.getClientIp())
                        + "，风险级别=" + defaultText(log.getRiskLevel())
                        + "，原因=" + detail
                        + "，时间=" + TIME_FORMATTER.format(log.getLoginAt()),
                recipients
        );
    }

    private String successRiskLevel(SysUser user, String clientIp, String deviceFingerprint) {
        LoginRiskLog previousSuccess = loginRiskLogMapper.selectOne(Wrappers.lambdaQuery(LoginRiskLog.class)
                .eq(LoginRiskLog::getUserId, user.getId())
                .eq(LoginRiskLog::getSuccessFlag, 1)
                .orderByDesc(LoginRiskLog::getLoginAt)
                .last("limit 1"));
        if (previousSuccess == null) {
            return "LOW";
        }
        boolean ipChanged = StringUtils.hasText(previousSuccess.getClientIp()) && StringUtils.hasText(clientIp) && !previousSuccess.getClientIp().equals(clientIp);
        boolean deviceChanged = StringUtils.hasText(previousSuccess.getDeviceFingerprint())
                && StringUtils.hasText(deviceFingerprint)
                && !previousSuccess.getDeviceFingerprint().equals(deviceFingerprint);
        return (ipChanged || deviceChanged) ? "MEDIUM" : "LOW";
    }

    private String successRiskReason(SysUser user, String clientIp, String deviceFingerprint) {
        LoginRiskLog previousSuccess = loginRiskLogMapper.selectOne(Wrappers.lambdaQuery(LoginRiskLog.class)
                .eq(LoginRiskLog::getUserId, user.getId())
                .eq(LoginRiskLog::getSuccessFlag, 1)
                .orderByDesc(LoginRiskLog::getLoginAt)
                .last("limit 1"));
        if (previousSuccess == null) {
            return "首次成功登录，已建立基础风险基线";
        }
        boolean ipChanged = StringUtils.hasText(previousSuccess.getClientIp()) && StringUtils.hasText(clientIp) && !previousSuccess.getClientIp().equals(clientIp);
        boolean deviceChanged = StringUtils.hasText(previousSuccess.getDeviceFingerprint())
                && StringUtils.hasText(deviceFingerprint)
                && !previousSuccess.getDeviceFingerprint().equals(deviceFingerprint);
        if (ipChanged && deviceChanged) {
            return "与最近一次成功登录相比，客户端 IP 和设备指纹均发生变化";
        }
        if (ipChanged) {
            return "与最近一次成功登录相比，客户端 IP 发生变化";
        }
        if (deviceChanged) {
            return "与最近一次成功登录相比，设备指纹发生变化";
        }
        return "成功登录，风险基线正常";
    }

    private String trimValue(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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

    private boolean configBoolean(String key, boolean defaultValue) {
        String raw = configValue(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    private String configValue(String key) {
        ConfigItem item = configItemMapper.selectOne(Wrappers.lambdaQuery(ConfigItem.class)
                .eq(ConfigItem::getConfigKey, key)
                .eq(ConfigItem::getConfigGroup, SECURITY_GROUP)
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

    private List<String> parseRoleCodes(String raw, String defaultValue) {
        String source = StringUtils.hasText(raw) ? raw : defaultValue;
        return List.of(source.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }
}
