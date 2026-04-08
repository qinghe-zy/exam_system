package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateWrongQuestionVO {

    private Long questionId;
    private String questionCode;
    private String questionType;
    private String stem;
    private String stemHtml;
    private String materialContent;
    private String attachmentJson;
    private String optionsJson;
    private String knowledgePoint;
    private String chapterName;
    private String referenceAnswer;
    private String analysisText;
    private Long latestRecordId;
    private String latestExamName;
    private String latestPaperName;
    private LocalDateTime latestSubmittedAt;
    private String latestAnswerContent;
    private Double latestMaxScore;
    private Double latestScoreAwarded;
    private Integer latestReviewLaterFlag;
    private Integer mistakeCount;
}
