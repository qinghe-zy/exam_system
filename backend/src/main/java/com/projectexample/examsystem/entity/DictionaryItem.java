package com.projectexample.examsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.projectexample.examsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_dictionary_item")
@EqualsAndHashCode(callSuper = true)
public class DictionaryItem extends BaseEntity {

    private String dictType;
    private String itemCode;
    private String itemLabel;
    private String itemValue;

    @TableField("sort_no")
    private Integer sortNo;

    private Integer status;
}
