package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DictionaryItemVO {

    private Long id;
    private String dictType;
    private String itemCode;
    private String itemLabel;
    private String itemValue;
    private Integer sortNo;
    private Integer status;
}
