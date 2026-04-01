package com.projectexample.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectexample.examsystem.entity.ExamCandidate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamCandidateMapper extends BaseMapper<ExamCandidate> {
}
