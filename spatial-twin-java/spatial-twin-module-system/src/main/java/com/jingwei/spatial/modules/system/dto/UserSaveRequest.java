package com.jingwei.spatial.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建或修改用户请求，支持一个用户关联多个角色。
 */
public record UserSaveRequest(
        @NotBlank(message = "请输入登录名")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{2,63}$", message = "登录名需以字母开头，仅包含字母、数字、点、短横线或下划线")
        String username,

        @NotBlank(message = "请输入用户姓名")
        @Size(max = 64, message = "用户姓名长度不能超过64位")
        String nickname,

        @NotNull(message = "请选择账号状态")
        Integer status,

        @NotEmpty(message = "请至少选择一个角色")
        List<Long> roleIds,

        @Size(min = 12, max = 128, message = "密码长度需为12至128位")
        String password
) {
}
