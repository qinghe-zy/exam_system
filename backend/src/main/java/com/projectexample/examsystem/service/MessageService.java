package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.InAppMessageVO;

import java.util.List;

public interface MessageService {

    List<InAppMessageVO> listCurrentMessages();

    void markRead(Long id);
}
