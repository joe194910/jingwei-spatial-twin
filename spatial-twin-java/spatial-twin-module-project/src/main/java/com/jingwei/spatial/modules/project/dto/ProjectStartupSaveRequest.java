package com.jingwei.spatial.modules.project.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 项目默认地图启动参数请求。
 */
public record ProjectStartupSaveRequest(
        @NotNull(message = "请输入默认中心经度")
        @DecimalMin(value = "-180", message = "默认中心经度不能小于-180")
        @DecimalMax(value = "180", message = "默认中心经度不能大于180")
        @Digits(integer = 3, fraction = 7, message = "默认中心经度最多保留7位小数")
        BigDecimal centerLng,

        @NotNull(message = "请输入默认中心纬度")
        @DecimalMin(value = "-90", message = "默认中心纬度不能小于-90")
        @DecimalMax(value = "90", message = "默认中心纬度不能大于90")
        @Digits(integer = 2, fraction = 7, message = "默认中心纬度最多保留7位小数")
        BigDecimal centerLat,

        @NotNull(message = "请输入默认缩放级别")
        @DecimalMin(value = "3", message = "默认缩放级别不能小于3")
        @DecimalMax(value = "20", message = "默认缩放级别不能大于20")
        BigDecimal zoomLevel,

        @NotNull(message = "请输入默认俯仰角")
        @DecimalMin(value = "0", message = "默认俯仰角不能小于0")
        @DecimalMax(value = "83", message = "默认俯仰角不能大于83")
        BigDecimal pitch,

        @NotNull(message = "请输入默认旋转角")
        @DecimalMin(value = "0", message = "默认旋转角不能小于0")
        @DecimalMax(value = "360", message = "默认旋转角不能大于360")
        BigDecimal rotation,

        @NotNull(message = "请选择地图模式")
        @Pattern(regexp = "^(2D|3D)$", message = "地图模式只能为2D或3D")
        String mapMode
) {
}
