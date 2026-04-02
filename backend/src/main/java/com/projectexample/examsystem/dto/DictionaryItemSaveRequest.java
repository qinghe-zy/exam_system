package com.projectexample.examsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictionaryItemSaveRequest {

    @NotBlank(message = "Dictionary type is required")
    private String dictType;

    @NotBlank(message = "Item code is required")
    private String itemCode;

    @NotBlank(message = "Item label is required")
    private String itemLabel;

    private String itemValue;

    @NotNull(message = "Sort order is required")
    private Integer sortNo;

    @NotNull(message = "Status is required")
    private Integer status;
}
