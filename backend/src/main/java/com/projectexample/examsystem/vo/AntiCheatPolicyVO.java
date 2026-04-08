package com.projectexample.examsystem.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AntiCheatPolicyVO {

    private Integer blockCopyEnabled;
    private Integer blockPasteEnabled;
    private Integer blockContextMenuEnabled;
    private Integer blockShortcutEnabled;
    private Integer deviceLoggingEnabled;
    private Integer deviceCheckEnabled;
    private Integer blockOnDeviceCheckFail;
    private Integer forbidMobileEntry;
    private Integer requireFullscreenSupport;
    private Integer minWindowWidth;
    private Integer minWindowHeight;
    private List<String> blockedShortcutKeys;
    private List<String> allowedBrowserKeywords;
}
