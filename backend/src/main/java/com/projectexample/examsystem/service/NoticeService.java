package com.projectexample.examsystem.service;

import com.projectexample.examsystem.common.PageResponse;
import com.projectexample.examsystem.dto.NoticeCreateRequest;
import com.projectexample.examsystem.dto.NoticeQueryRequest;
import com.projectexample.examsystem.dto.NoticeUpdateRequest;
import com.projectexample.examsystem.vo.NoticeVO;

public interface NoticeService {

    PageResponse<NoticeVO> pageNotices(NoticeQueryRequest request);

    NoticeVO getNotice(Long id);

    NoticeVO createNotice(NoticeCreateRequest request);

    NoticeVO updateNotice(Long id, NoticeUpdateRequest request);

    void deleteNotice(Long id);
}
