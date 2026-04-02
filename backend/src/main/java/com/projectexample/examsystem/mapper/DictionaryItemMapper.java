package com.projectexample.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.projectexample.examsystem.entity.DictionaryItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictionaryItemMapper extends BaseMapper<DictionaryItem> {
}
