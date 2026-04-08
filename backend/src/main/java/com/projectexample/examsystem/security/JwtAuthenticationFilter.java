package com.projectexample.examsystem.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && !jwtTokenProvider.isValid(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session token is invalid");
            return;
        }
        if (StringUtils.hasText(token) && jwtTokenProvider.isValid(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            SysUser currentUser = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                    .eq(SysUser::getUsername, claims.getSubject())
                    .eq(SysUser::getStatus, 1)
                    .last("limit 1"));
            Integer tokenSessionVersion = claims.get("sessionVersion", Integer.class);
            if (currentUser == null || !matchesSessionVersion(currentUser, tokenSessionVersion)) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session token has expired");
                return;
            }
            UserPrincipal userPrincipal = new UserPrincipal(
                    claims.getSubject(),
                    claims.get("nickname", String.class),
                    claims.get("roleCode", String.class),
                    tokenSessionVersion == null ? 0 : tokenSessionVersion
            );
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private boolean matchesSessionVersion(SysUser user, Integer tokenSessionVersion) {
        int current = user.getSessionVersion() == null ? 0 : user.getSessionVersion();
        int claimed = tokenSessionVersion == null ? 0 : tokenSessionVersion;
        return current == claimed;
    }
}
