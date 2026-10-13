package com.jingwei.spatial.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建或修改角色请求，菜单勾选最终转换为稳定权限码关联。
 */
public record RoleSaveRequest(
        @NotBlank(message = "请输入角色编码")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,63}$", message = "角色编码仅支持大写字母、数字和下划线")
        String code,

        @NotBlank(message = "请输入角色名称")
        @Size(max = 64, message = "角色名称长度不能超过64位")
        String name,

        @NotNull(message = "请选择角色状态")
        Integer status,

        @Size(max = 255, message = "角色说明长度不能超过255位")
        String description,

        List<Long> menuIds
) {
}
