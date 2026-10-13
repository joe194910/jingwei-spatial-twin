package com.jingwei.spatial.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码登录请求。
 */
public record LoginRequest(
        @NotBlank(message = "请输入用户名")
        @Size(max = 64, message = "用户名长度不能超过64位")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(max = 128, message = "密码长度不能超过128位")
        String password
) {
}
