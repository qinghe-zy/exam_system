package com.projectexample.examsystem.service;

import com.projectexample.examsystem.vo.AntiCheatEventVO;

import java.util.List;

public interface AntiCheatService {

    List<AntiCheatEventVO> listEvents();
}
