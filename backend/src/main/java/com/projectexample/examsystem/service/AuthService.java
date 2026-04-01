package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.LoginRequest;
import com.projectexample.examsystem.vo.AuthTokenVO;
import com.projectexample.examsystem.vo.CurrentUserVO;

public interface AuthService {

    AuthTokenVO login(LoginRequest request);

    CurrentUserVO currentUser(String username);
}
