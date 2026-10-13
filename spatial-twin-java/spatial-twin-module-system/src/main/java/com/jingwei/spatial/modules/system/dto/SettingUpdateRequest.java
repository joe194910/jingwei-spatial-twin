package com.jingwei.spatial.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改系统单值配置请求。
 */
public record SettingUpdateRequest(
        @NotBlank(message = "请输入配置值")
        @Size(max = 512, message = "配置值长度不能超过512位")
        String value
) {
}
