package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.AuditLogVO;

import java.util.List;

public interface AuditLogService {

    List<AuditLogVO> listLogs();
}
