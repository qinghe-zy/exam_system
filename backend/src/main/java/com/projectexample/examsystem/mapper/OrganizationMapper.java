package com.projectexample.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectexample.examsystem.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
