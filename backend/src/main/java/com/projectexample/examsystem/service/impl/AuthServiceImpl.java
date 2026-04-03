package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.LoginRequest;
import com.projectexample.examsystem.dto.PasswordResetRequest;
import com.projectexample.examsystem.dto.RegisterRequest;
import com.projectexample.examsystem.dto.SendVerificationCodeRequest;
import com.projectexample.examsystem.entity.Organization;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.entity.VerificationCode;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.OrganizationMapper;
import com.projectexample.examsystem.mapper.SysUserMapper;
import com.projectexample.examsystem.mapper.VerificationCodeMapper;
import com.projectexample.examsystem.security.JwtTokenProvider;
import com.projectexample.examsystem.security.RolePermissionCatalog;
import com.projectexample.examsystem.service.AuthService;
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

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;
    private final OrganizationMapper organizationMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RolePermissionCatalog rolePermissionCatalog;

    @Value("${app.auth.verification.expire-minutes:10}")
    private long verificationExpireMinutes;

    @Value("${app.auth.verification.mock-enabled:true}")
    private boolean verificationMockEnabled;

    @Override
    public AuthTokenVO login(LoginRequest request) {
        SysUser user = sysUserService.findByUsername(request.getUsername());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("Invalid username or password");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getNickname(), user.getRoleCode());
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
        sysUserMapper.updateById(user);
        markVerificationCodeConsumed(verificationCode);
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

    private void assertTargetMatchesUser(String channel, String targetValue, SysUser user) {
        String actual = resolveResetTarget(channel, user);
        if (!actual.equalsIgnoreCase(targetValue)) {
            throw new BusinessException(4004, "验证码接收目标与当前账号绑定信息不一致");
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
}
