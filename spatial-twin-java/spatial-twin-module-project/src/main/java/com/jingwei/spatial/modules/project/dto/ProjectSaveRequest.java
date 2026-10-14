package com.jingwei.spatial.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建或修改园区项目请求。
 */
public record ProjectSaveRequest(
        @NotBlank(message = "请输入项目编码")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{2,63}$", message = "项目编码需以字母开头，仅包含字母、数字、短横线或下划线")
        String projectCode,

        @NotBlank(message = "请输入项目名称")
        @Size(max = 128, message = "项目名称长度不能超过128位")
        String name,

        @Size(max = 500, message = "项目说明长度不能超过500位")
        String description,

        @NotBlank(message = "请输入项目位置名称")
        @Size(max = 255, message = "项目位置名称长度不能超过255位")
        String locationName,

        @NotNull(message = "请设置默认地图参数")
        @Valid
        ProjectStartupSaveRequest startup
) {
}
