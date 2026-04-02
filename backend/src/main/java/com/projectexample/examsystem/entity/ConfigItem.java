package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_config_item")
@EqualsAndHashCode(callSuper = true)
public class ConfigItem extends BaseEntity {

    private String configKey;
    private String configName;
    private String configGroup;
    private String configValue;
    private String descriptionText;
    private Integer status;
}
