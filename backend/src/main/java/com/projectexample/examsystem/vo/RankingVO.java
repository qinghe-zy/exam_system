package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RankingVO {

    private Integer rankNo;
    private String candidateName;
    private String examName;
    private Double finalScore;
}
