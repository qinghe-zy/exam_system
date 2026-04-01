package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.ExamRecord;
import com.projectexample.examsystem.mapper.ExamRecordMapper;
import com.projectexample.examsystem.service.ExamRecordService;
import com.projectexample.examsystem.vo.ExamRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamRecordServiceImpl implements ExamRecordService {

    private final ExamRecordMapper examRecordMapper;

    @Override
    public List<ExamRecordVO> listRecords() {
        return examRecordMapper.selectList(Wrappers.lambdaQuery(ExamRecord.class).orderByDesc(ExamRecord::getSubmittedAt, ExamRecord::getId))
                .stream()
                .map(record -> ExamRecordVO.builder()
                        .id(record.getId())
                        .candidateName(record.getCandidateName())
                        .examName(record.getExamName())
                        .paperName(record.getPaperName())
                        .submittedAt(record.getSubmittedAt())
                        .objectiveScore(record.getObjectiveScore())
                        .subjectiveScore(record.getSubjectiveScore())
                        .finalScore(record.getFinalScore())
                        .passedFlag(record.getPassedFlag())
                        .publishedFlag(record.getPublishedFlag())
                        .status(record.getStatus())
                        .build())
                .toList();
    }
}
