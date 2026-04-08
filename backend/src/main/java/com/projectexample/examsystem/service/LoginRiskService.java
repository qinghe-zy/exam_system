package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.LoginRiskLogVO;

import java.util.List;

public interface LoginRiskService {

    List<LoginRiskLogVO> listLogs();
}
