package com.jingwei.spatial.modules.project.vo;

import java.math.BigDecimal;

/**
 * 项目默认地图启动参数输出。
 */
public record ProjectStartupVO(
        String mapProvider,
        String mapMode,
        BigDecimal centerLng,
        BigDecimal centerLat,
        BigDecimal zoomLevel,
        BigDecimal pitch,
        BigDecimal rotation
) {
}
