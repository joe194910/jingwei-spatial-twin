package com.jingwei.spatial.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置用户密码请求。
 */
public record PasswordResetRequest(
        @NotBlank(message = "请输入新密码")
        @Size(min = 12, max = 128, message = "密码长度需为12至128位")
        String password
) {
}
