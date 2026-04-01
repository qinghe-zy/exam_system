package com.projectexample.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectexample.examsystem.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
