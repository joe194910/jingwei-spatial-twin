package com.jingwei.spatial.modules.system.vo;

import java.time.LocalDateTime;

/**
 * 地图供应商管理配置输出，敏感凭证不返回明文。
 */
public record MapProviderConfigVO(
        String providerCode,
        boolean enabled,
        String apiVersion,
        boolean webKeyConfigured,
        boolean securityJsCodeConfigured,
        String mapStyle,
        String viewMode,
        int defaultPitch,
        int defaultRotation,
        int minZoom,
        int maxZoom,
        boolean showScale,
        boolean showToolbar,
        boolean showControlBar,
        boolean encryptionReady,
        LocalDateTime updatedAt
) {
}
