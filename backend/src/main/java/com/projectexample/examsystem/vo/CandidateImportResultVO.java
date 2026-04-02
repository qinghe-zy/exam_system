package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CandidateImportResultVO {

    private Integer importedCount;
    private List<String> usernames;
}
