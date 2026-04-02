package com.projectexample.examsystem.service;

import com.projectexample.examsystem.dto.ConfigItemSaveRequest;
import com.projectexample.examsystem.dto.DictionaryItemSaveRequest;
import com.projectexample.examsystem.vo.ConfigItemVO;
import com.projectexample.examsystem.vo.DictionaryItemVO;

import java.util.List;

public interface ConfigCenterService {

    List<ConfigItemVO> listConfigs();

    ConfigItemVO createConfig(ConfigItemSaveRequest request);

    ConfigItemVO updateConfig(Long id, ConfigItemSaveRequest request);

    void deleteConfig(Long id);

    List<DictionaryItemVO> listDictionaries();

    DictionaryItemVO createDictionary(DictionaryItemSaveRequest request);

    DictionaryItemVO updateDictionary(Long id, DictionaryItemSaveRequest request);

    void deleteDictionary(Long id);
}
