package com.jingwei.spatial.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建或修改后台菜单请求。
 */
public record MenuSaveRequest(
        Long parentId,

        @NotBlank(message = "请输入菜单名称")
        @Size(max = 64, message = "菜单名称长度不能超过64位")
        String name,

        @NotBlank(message = "请输入菜单路径")
        @Size(max = 160, message = "菜单路径长度不能超过160位")
        String path,

        @Size(max = 160, message = "组件标识长度不能超过160位")
        String component,

        @Size(max = 64, message = "图标名称长度不能超过64位")
        String icon,

        @NotNull(message = "请输入菜单排序")
        @Min(value = 0, message = "菜单排序不能小于0")
        @Max(value = 9999, message = "菜单排序不能大于9999")
        Integer sortOrder,

        @Pattern(regexp = "^$|^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$", message = "权限码格式应为模块:对象:动作")
        String permissionCode,

        @NotNull(message = "请选择菜单是否可见")
        Integer visible
) {
}
