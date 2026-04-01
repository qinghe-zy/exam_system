package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectexample.examsystem.common.PageResponse;
import com.projectexample.examsystem.dto.NoticeCreateRequest;
import com.projectexample.examsystem.dto.NoticeQueryRequest;
import com.projectexample.examsystem.dto.NoticeUpdateRequest;
import com.projectexample.examsystem.entity.Notice;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.NoticeMapper;
import com.projectexample.examsystem.service.NoticeService;
import com.projectexample.examsystem.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public PageResponse<NoticeVO> pageNotices(NoticeQueryRequest request) {
        Page<Notice> page = noticeMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                Wrappers.lambdaQuery(Notice.class)
                        .like(StringUtils.hasText(request.getTitle()), Notice::getTitle, request.getTitle())
                        .eq(request.getStatus() != null, Notice::getStatus, request.getStatus())
                        .orderByDesc(Notice::getUpdateTime)
        );
        return PageResponse.of(page, this::toVO);
    }

    @Override
    public NoticeVO getNotice(Long id) {
        return toVO(requireNotice(id));
    }

    @Override
    public NoticeVO createNotice(NoticeCreateRequest request) {
        Notice notice = new Notice();
        notice.setTitle(request.getTitle());
        notice.setCategory(request.getCategory());
        notice.setStatus(request.getStatus());
        notice.setContent(request.getContent());
        notice.setPublishTime(LocalDateTime.now());
        noticeMapper.insert(notice);
        return getNotice(notice.getId());
    }

    @Override
    public NoticeVO updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = requireNotice(id);
        notice.setTitle(request.getTitle());
        notice.setCategory(request.getCategory());
        notice.setStatus(request.getStatus());
        notice.setContent(request.getContent());
        noticeMapper.updateById(notice);
        return getNotice(id);
    }

    @Override
    public void deleteNotice(Long id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(4040, "Notice record not found");
        }
        return notice;
    }

    private NoticeVO toVO(Notice notice) {
        return NoticeVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .category(notice.getCategory())
                .status(notice.getStatus())
                .content(notice.getContent())
                .publishTime(notice.getPublishTime())
                .updateTime(notice.getUpdateTime())
                .build();
    }
}
