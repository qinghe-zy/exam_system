package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.dto.ConfigItemSaveRequest;
import com.projectexample.examsystem.dto.DictionaryItemSaveRequest;
import com.projectexample.examsystem.service.ConfigCenterService;
import com.projectexample.examsystem.vo.ConfigItemVO;
import com.projectexample.examsystem.vo.DictionaryItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/config-center")
@RequiredArgsConstructor
public class ConfigCenterController {

    private final ConfigCenterService configCenterService;

    @GetMapping("/configs")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<List<ConfigItemVO>> configs() {
        return ApiResponse.success(configCenterService.listConfigs());
    }

    @PostMapping("/configs")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<ConfigItemVO> createConfig(@Valid @RequestBody ConfigItemSaveRequest request) {
        return ApiResponse.success("config created", configCenterService.createConfig(request));
    }

    @PutMapping("/configs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<ConfigItemVO> updateConfig(@PathVariable Long id, @Valid @RequestBody ConfigItemSaveRequest request) {
        return ApiResponse.success("config updated", configCenterService.updateConfig(id, request));
    }

    @DeleteMapping("/configs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        configCenterService.deleteConfig(id);
        return ApiResponse.success("config deleted", null);
    }

    @GetMapping("/dictionaries")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<List<DictionaryItemVO>> dictionaries() {
        return ApiResponse.success(configCenterService.listDictionaries());
    }

    @PostMapping("/dictionaries")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<DictionaryItemVO> createDictionary(@Valid @RequestBody DictionaryItemSaveRequest request) {
        return ApiResponse.success("dictionary item created", configCenterService.createDictionary(request));
    }

    @PutMapping("/dictionaries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<DictionaryItemVO> updateDictionary(@PathVariable Long id, @Valid @RequestBody DictionaryItemSaveRequest request) {
        return ApiResponse.success("dictionary item updated", configCenterService.updateDictionary(id, request));
    }

    @DeleteMapping("/dictionaries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
    public ApiResponse<Void> deleteDictionary(@PathVariable Long id) {
        configCenterService.deleteDictionary(id);
        return ApiResponse.success("dictionary item deleted", null);
    }
}
