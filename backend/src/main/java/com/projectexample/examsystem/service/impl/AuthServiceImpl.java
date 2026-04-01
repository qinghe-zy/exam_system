package com.projectexample.examsystem.service.impl;

import com.projectexample.examsystem.dto.LoginRequest;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.security.JwtTokenProvider;
import com.projectexample.examsystem.service.AuthService;
import com.projectexample.examsystem.service.SysUserService;
import com.projectexample.examsystem.vo.AuthTokenVO;
import com.projectexample.examsystem.vo.CurrentUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthTokenVO login(LoginRequest request) {
        SysUser user = sysUserService.findByUsername(request.getUsername());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("Invalid username or password");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        CurrentUserVO currentUser = CurrentUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .fullName(user.getFullName())
                .roleCode(user.getRoleCode())
                .organizationName(user.getOrganizationName())
                .build();
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getNickname(), user.getRoleCode());
        return new AuthTokenVO(token, currentUser);
    }

    @Override
    public CurrentUserVO currentUser(String username) {
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(4010, "User session is invalid");
        }
        return CurrentUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .fullName(user.getFullName())
                .roleCode(user.getRoleCode())
                .organizationName(user.getOrganizationName())
                .build();
    }
}
