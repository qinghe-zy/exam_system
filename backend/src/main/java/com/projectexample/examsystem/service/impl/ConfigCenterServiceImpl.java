package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.dto.ConfigItemSaveRequest;
import com.projectexample.examsystem.dto.DictionaryItemSaveRequest;
import com.projectexample.examsystem.entity.AnswerSheet;
import com.projectexample.examsystem.entity.ConfigItem;
import com.projectexample.examsystem.entity.DictionaryItem;
import com.projectexample.examsystem.entity.ExamPlan;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.AnswerSheetMapper;
import com.projectexample.examsystem.mapper.ConfigItemMapper;
import com.projectexample.examsystem.mapper.DictionaryItemMapper;
import com.projectexample.examsystem.mapper.ExamPlanMapper;
import com.projectexample.examsystem.service.ConfigCenterService;
import com.projectexample.examsystem.vo.ConfigItemVO;
import com.projectexample.examsystem.vo.DictionaryItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConfigCenterServiceImpl implements ConfigCenterService {

    private final ConfigItemMapper configItemMapper;
    private final DictionaryItemMapper dictionaryItemMapper;
    private final ExamPlanMapper examPlanMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private static final Set<String> PROTECTED_CONFIG_GROUPS = Set.of("exam", "anti_cheat");

    @Override
    public List<ConfigItemVO> listConfigs() {
        return configItemMapper.selectList(Wrappers.lambdaQuery(ConfigItem.class).orderByAsc(ConfigItem::getConfigGroup, ConfigItem::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public ConfigItemVO createConfig(ConfigItemSaveRequest request) {
        assertConfigMutable(request.getConfigGroup(), "新增");
        ConfigItem item = new ConfigItem();
        apply(item, request);
        configItemMapper.insert(item);
        return toVO(requireConfig(item.getId()));
    }

    @Override
    public ConfigItemVO updateConfig(Long id, ConfigItemSaveRequest request) {
        ConfigItem item = requireConfig(id);
        assertConfigMutable(item.getConfigGroup(), "更新");
        apply(item, request);
        configItemMapper.updateById(item);
        return toVO(requireConfig(id));
    }

    @Override
    public void deleteConfig(Long id) {
        ConfigItem item = requireConfig(id);
        assertConfigMutable(item.getConfigGroup(), "删除");
        configItemMapper.deleteById(id);
    }

    @Override
    public List<DictionaryItemVO> listDictionaries() {
        return dictionaryItemMapper.selectList(Wrappers.lambdaQuery(DictionaryItem.class).orderByAsc(DictionaryItem::getDictType, DictionaryItem::getSortNo, DictionaryItem::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public DictionaryItemVO createDictionary(DictionaryItemSaveRequest request) {
        DictionaryItem item = new DictionaryItem();
        apply(item, request);
        dictionaryItemMapper.insert(item);
        return toVO(requireDictionary(item.getId()));
    }

    @Override
    public DictionaryItemVO updateDictionary(Long id, DictionaryItemSaveRequest request) {
        DictionaryItem item = requireDictionary(id);
        apply(item, request);
        dictionaryItemMapper.updateById(item);
        return toVO(requireDictionary(id));
    }

    @Override
    public void deleteDictionary(Long id) {
        requireDictionary(id);
        dictionaryItemMapper.deleteById(id);
    }

    private ConfigItem requireConfig(Long id) {
        ConfigItem item = configItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(4040, "Configuration not found");
        }
        return item;
    }

    private DictionaryItem requireDictionary(Long id) {
        DictionaryItem item = dictionaryItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(4040, "Dictionary item not found");
        }
        return item;
    }

    private void apply(ConfigItem item, ConfigItemSaveRequest request) {
        item.setConfigKey(request.getConfigKey());
        item.setConfigName(request.getConfigName());
        item.setConfigGroup(request.getConfigGroup());
        item.setConfigValue(request.getConfigValue());
        item.setDescriptionText(request.getDescriptionText());
        item.setStatus(request.getStatus());
    }

    private void apply(DictionaryItem item, DictionaryItemSaveRequest request) {
        item.setDictType(request.getDictType());
        item.setItemCode(request.getItemCode());
        item.setItemLabel(request.getItemLabel());
        item.setItemValue(request.getItemValue());
        item.setSortNo(request.getSortNo());
        item.setStatus(request.getStatus());
    }

    private ConfigItemVO toVO(ConfigItem item) {
        return ConfigItemVO.builder()
                .id(item.getId())
                .configKey(item.getConfigKey())
                .configName(item.getConfigName())
                .configGroup(item.getConfigGroup())
                .configValue(item.getConfigValue())
                .descriptionText(item.getDescriptionText())
                .status(item.getStatus())
                .build();
    }

    private DictionaryItemVO toVO(DictionaryItem item) {
        return DictionaryItemVO.builder()
                .id(item.getId())
                .dictType(item.getDictType())
                .itemCode(item.getItemCode())
                .itemLabel(item.getItemLabel())
                .itemValue(item.getItemValue())
                .sortNo(item.getSortNo())
                .status(item.getStatus())
                .build();
    }

    private void assertConfigMutable(String configGroup, String actionName) {
        if (!PROTECTED_CONFIG_GROUPS.contains(String.valueOf(configGroup))) {
            return;
        }
        List<ExamPlan> activePlans = examPlanMapper.selectList(Wrappers.lambdaQuery(ExamPlan.class)
                .eq(ExamPlan::getPublishStatus, 1));
        boolean started = activePlans.stream().anyMatch(plan -> plan.getStartTime() != null && !LocalDateTime.now().isBefore(plan.getStartTime()));
        if (started) {
            throw new BusinessException(4005, "当前存在已开始考试，暂不允许" + actionName + "高风险配置");
        }
        List<Long> planIds = activePlans.stream().map(ExamPlan::getId).toList();
        if (!planIds.isEmpty()) {
            long answerSheetCount = answerSheetMapper.selectCount(Wrappers.lambdaQuery(AnswerSheet.class)
                    .in(AnswerSheet::getExamPlanId, planIds));
            if (answerSheetCount > 0) {
                throw new BusinessException(4005, "当前已存在考试答卷，暂不允许" + actionName + "高风险配置");
            }
        }
    }
}
