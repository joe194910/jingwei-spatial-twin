package com.jingwei.spatial.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.system.dto.MapProviderConfigUpdateRequest;
import com.jingwei.spatial.modules.system.service.MapProviderConfigService;
import com.jingwei.spatial.modules.system.vo.MapProviderConfigVO;
import com.jingwei.spatial.modules.system.vo.MapRuntimeConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地图供应商管理配置和首页运行时配置接口。
 */
@RestController
@RequestMapping("/api/v1")
public class MapProviderConfigController {

    private final MapProviderConfigService configService;

    public MapProviderConfigController(MapProviderConfigService configService) {
        this.configService = configService;
    }

    /**
     * 查询超级管理员可编辑的地图供应商配置。
     *
     * @return 脱敏后的地图配置
     */
    @GetMapping("/system/map-provider-config")
    @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<MapProviderConfigVO> getAdminConfig() {
        return ApiResponse.success(configService.getAdminConfig());
    }

    /**
     * 修改地图供应商配置。
     *
     * @param request 地图配置
     * @return 空数据成功响应
     */
    @PutMapping("/system/map-provider-config")
    @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<Void> update(@Valid @RequestBody MapProviderConfigUpdateRequest request) {
        configService.update(request);
        return ApiResponse.success("地图底座配置修改成功", null);
    }

    /**
     * 查询首页地图适配器运行参数，无在线条件时返回本地底座降级原因。
     *
     * @return 地图运行配置
     */
    @GetMapping("/map/runtime-config")
    @SaCheckPermission("workspace:dashboard:view")
    public ApiResponse<MapRuntimeConfigVO> getRuntimeConfig() {
        return ApiResponse.success(configService.getRuntimeConfig());
    }
}
