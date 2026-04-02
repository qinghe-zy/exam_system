package com.projectexample.examsystem.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.projectexample.examsystem.entity.InAppMessage;
import com.projectexample.examsystem.entity.SysUser;
import com.projectexample.examsystem.exception.BusinessException;
import com.projectexample.examsystem.mapper.InAppMessageMapper;
import com.projectexample.examsystem.security.AccessScopeService;
import com.projectexample.examsystem.service.MessageService;
import com.projectexample.examsystem.vo.InAppMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final InAppMessageMapper inAppMessageMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public List<InAppMessageVO> listCurrentMessages() {
        SysUser user = accessScopeService.currentUser();
        return inAppMessageMapper.selectList(Wrappers.lambdaQuery(InAppMessage.class)
                        .eq(InAppMessage::getRecipientUserId, user.getId())
                        .orderByDesc(InAppMessage::getCreateTime, InAppMessage::getId))
                .stream()
                .map(item -> InAppMessageVO.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .messageType(item.getMessageType())
                        .content(item.getContent())
                        .relatedType(item.getRelatedType())
                        .relatedId(item.getRelatedId())
                        .readFlag(item.getReadFlag())
                        .createTime(item.getCreateTime())
                        .build())
                .toList();
    }

    @Override
    public void markRead(Long id) {
        SysUser user = accessScopeService.currentUser();
        InAppMessage message = inAppMessageMapper.selectById(id);
        if (message == null || !user.getId().equals(message.getRecipientUserId())) {
            throw new BusinessException(4040, "Message not found");
        }
        message.setReadFlag(1);
        inAppMessageMapper.updateById(message);
    }
}
