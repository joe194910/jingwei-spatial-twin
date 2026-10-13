package com.jingwei.spatial.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.system.dto.SettingUpdateRequest;
import com.jingwei.spatial.modules.system.service.SystemSettingService;
import com.jingwei.spatial.modules.system.vo.SettingVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 超级管理员专用的系统单值配置接口。
 */
@RestController
@RequestMapping("/api/v1/system/settings")
@SaCheckRole("SUPER_ADMIN")
public class SystemSettingController {

    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ApiResponse<List<SettingVO>> list() {
        return ApiResponse.success(settingService.list());
    }

    @PutMapping
    public ApiResponse<Void> update(
            @RequestParam String key,
            @Valid @RequestBody SettingUpdateRequest request
    ) {
        settingService.update(key, request.value());
        return ApiResponse.success("系统配置修改成功", null);
    }
}
