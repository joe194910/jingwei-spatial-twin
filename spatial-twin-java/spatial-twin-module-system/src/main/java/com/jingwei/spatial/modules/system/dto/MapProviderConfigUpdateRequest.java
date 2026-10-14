package com.jingwei.spatial.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改地图供应商配置请求。
 */
public record MapProviderConfigUpdateRequest(
        @NotNull(message = "请选择是否启用在线地图")
        Boolean enabled,
        @NotBlank(message = "请选择地图API版本")
        @Pattern(regexp = "2\\.0", message = "当前仅支持稳定版高德JS API 2.0")
        String apiVersion,
        @Size(max = 128, message = "高德Web端Key长度不能超过128位")
        String webKey,
        @NotNull(message = "缺少Web端Key清除标记")
        Boolean clearWebKey,
        @Size(max = 255, message = "高德安全密钥长度不能超过255位")
        String securityJsCode,
        @NotNull(message = "缺少安全密钥清除标记")
        Boolean clearSecurityJsCode,
        @Size(max = 160, message = "地图样式标识长度不能超过160位")
        String mapStyle,
        @NotBlank(message = "请选择地图视图模式")
        @Pattern(regexp = "2D|3D", message = "地图视图模式只能是2D或3D")
        String viewMode,
        @NotNull(message = "请输入默认俯仰角")
        @Min(value = 0, message = "默认俯仰角不能小于0度")
        @Max(value = 83, message = "默认俯仰角不能大于83度")
        Integer defaultPitch,
        @NotNull(message = "请输入默认旋转角")
        @Min(value = 0, message = "默认旋转角不能小于0度")
        @Max(value = 359, message = "默认旋转角不能大于359度")
        Integer defaultRotation,
        @NotNull(message = "请输入最小缩放级别")
        @Min(value = 2, message = "最小缩放级别不能小于2")
        @Max(value = 20, message = "最小缩放级别不能大于20")
        Integer minZoom,
        @NotNull(message = "请输入最大缩放级别")
        @Min(value = 2, message = "最大缩放级别不能小于2")
        @Max(value = 20, message = "最大缩放级别不能大于20")
        Integer maxZoom,
        @NotNull(message = "请选择是否显示比例尺")
        Boolean showScale,
        @NotNull(message = "请选择是否显示缩放工具")
        Boolean showToolbar,
        @NotNull(message = "请选择是否显示旋转控件")
        Boolean showControlBar
) {
}
