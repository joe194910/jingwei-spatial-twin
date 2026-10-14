package com.jingwei.spatial.modules.system.vo;

/**
 * 首页地图适配器运行配置。在线地图不可用时，前端按 fallbackMode 启用本地底座。
 */
public record MapRuntimeConfigVO(
        boolean onlineEnabled,
        String providerCode,
        String apiVersion,
        String webKey,
        String securityJsCode,
        String mapStyle,
        String viewMode,
        int defaultPitch,
        int defaultRotation,
        int minZoom,
        int maxZoom,
        boolean showScale,
        boolean showToolbar,
        boolean showControlBar,
        String fallbackMode,
        String unavailableReason
) {
}
