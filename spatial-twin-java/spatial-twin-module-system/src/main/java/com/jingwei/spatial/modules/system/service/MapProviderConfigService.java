package com.jingwei.spatial.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.system.dto.MapProviderConfigUpdateRequest;
import com.jingwei.spatial.modules.system.entity.MapProviderConfigEntity;
import com.jingwei.spatial.modules.system.mapper.MapProviderConfigMapper;
import com.jingwei.spatial.modules.system.vo.MapProviderConfigVO;
import com.jingwei.spatial.modules.system.vo.MapRuntimeConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 管理统一地图供应商配置，并生成首页地图适配器所需的运行参数。
 */
@Service
public class MapProviderConfigService {

    private static final long CONFIG_ID = 1L;
    private static final String FALLBACK_MODE = "JINGWEI_OFFLINE";

    private final MapProviderConfigMapper configMapper;
    private final MapCredentialCipher credentialCipher;
    private final SystemAuditService auditService;

    public MapProviderConfigService(
            MapProviderConfigMapper configMapper,
            MapCredentialCipher credentialCipher,
            SystemAuditService auditService
    ) {
        this.configMapper = configMapper;
        this.credentialCipher = credentialCipher;
        this.auditService = auditService;
    }

    /**
     * 查询管理员可编辑的地图配置，凭证只返回是否已配置。
     *
     * @return 地图供应商配置
     */
    public MapProviderConfigVO getAdminConfig() {
        MapProviderConfigEntity entity = getRequired();
        return new MapProviderConfigVO(
                entity.getProviderCode(),
                enabled(entity.getEnabled()),
                entity.getApiVersion(),
                hasText(entity.getWebKeyCiphertext()),
                hasText(entity.getSecurityJsCodeCiphertext()),
                entity.getMapStyle(),
                entity.getViewMode(),
                entity.getDefaultPitch(),
                entity.getDefaultRotation(),
                entity.getMinZoom(),
                entity.getMaxZoom(),
                enabled(entity.getShowScale()),
                enabled(entity.getShowToolbar()),
                enabled(entity.getShowControlBar()),
                credentialCipher.isReady(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 更新地图供应商配置。空凭证表示保留原值，只有显式清除标记才会删除密文。
     *
     * @param request 地图配置
     */
    @Transactional
    public void update(MapProviderConfigUpdateRequest request) {
        if (request.minZoom() > request.maxZoom()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "最小缩放级别不能大于最大缩放级别");
        }

        MapProviderConfigEntity entity = getRequired();
        String webKey = normalize(request.webKey());
        String securityJsCode = normalize(request.securityJsCode());
        if (Boolean.TRUE.equals(request.clearWebKey())) {
            entity.setWebKeyCiphertext(null);
        } else if (hasText(webKey)) {
            entity.setWebKeyCiphertext(credentialCipher.encrypt(webKey));
        }
        if (Boolean.TRUE.equals(request.clearSecurityJsCode())) {
            entity.setSecurityJsCodeCiphertext(null);
        } else if (hasText(securityJsCode)) {
            entity.setSecurityJsCodeCiphertext(credentialCipher.encrypt(securityJsCode));
        }

        boolean willHaveWebKey = hasText(entity.getWebKeyCiphertext());
        boolean willHaveSecurityCode = hasText(entity.getSecurityJsCodeCiphertext());
        if (Boolean.TRUE.equals(request.enabled()) && (!willHaveWebKey || !willHaveSecurityCode)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "启用高德地图前请先配置Web端Key和安全密钥");
        }

        entity.setEnabled(booleanValue(request.enabled()));
        entity.setApiVersion(request.apiVersion());
        entity.setMapStyle(normalize(request.mapStyle()));
        entity.setViewMode(request.viewMode());
        entity.setDefaultPitch(request.defaultPitch());
        entity.setDefaultRotation(request.defaultRotation());
        entity.setMinZoom(request.minZoom());
        entity.setMaxZoom(request.maxZoom());
        entity.setShowScale(booleanValue(request.showScale()));
        entity.setShowToolbar(booleanValue(request.showToolbar()));
        entity.setShowControlBar(booleanValue(request.showControlBar()));
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(currentUserId());
        if (configMapper.updateById(entity) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "地图配置已被其他操作修改，请刷新后重试");
        }
        auditService.record("MAP_PROVIDER_CONFIG", String.valueOf(CONFIG_ID),
                "UPDATE", "SUCCESS", "地图供应商配置已更新，凭证未写入审计详情");
    }

    /**
     * 生成前端地图适配器运行配置。任一在线条件不满足时返回本地底座降级原因。
     *
     * @return 地图运行配置
     */
    public MapRuntimeConfigVO getRuntimeConfig() {
        MapProviderConfigEntity entity = getRequired();
        if (!enabled(entity.getEnabled())) {
            return offline(entity, "在线地图未启用");
        }
        if (!hasText(entity.getWebKeyCiphertext()) || !hasText(entity.getSecurityJsCodeCiphertext())) {
            return offline(entity, "在线地图凭证尚未配置完整");
        }
        if (!credentialCipher.isReady()) {
            return offline(entity, "服务器未配置地图凭证解密主密钥");
        }
        try {
            return new MapRuntimeConfigVO(
                    true,
                    entity.getProviderCode(),
                    entity.getApiVersion(),
                    credentialCipher.decrypt(entity.getWebKeyCiphertext()),
                    credentialCipher.decrypt(entity.getSecurityJsCodeCiphertext()),
                    entity.getMapStyle(),
                    entity.getViewMode(),
                    entity.getDefaultPitch(),
                    entity.getDefaultRotation(),
                    entity.getMinZoom(),
                    entity.getMaxZoom(),
                    enabled(entity.getShowScale()),
                    enabled(entity.getShowToolbar()),
                    enabled(entity.getShowControlBar()),
                    FALLBACK_MODE,
                    null
            );
        } catch (BusinessException exception) {
            return offline(entity, "地图凭证无法解密");
        }
    }

    private MapRuntimeConfigVO offline(MapProviderConfigEntity entity, String reason) {
        return new MapRuntimeConfigVO(
                false,
                entity.getProviderCode(),
                entity.getApiVersion(),
                null,
                null,
                entity.getMapStyle(),
                entity.getViewMode(),
                entity.getDefaultPitch(),
                entity.getDefaultRotation(),
                entity.getMinZoom(),
                entity.getMaxZoom(),
                enabled(entity.getShowScale()),
                enabled(entity.getShowToolbar()),
                enabled(entity.getShowControlBar()),
                FALLBACK_MODE,
                reason
        );
    }

    private MapProviderConfigEntity getRequired() {
        MapProviderConfigEntity entity = configMapper.selectById(CONFIG_ID);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地图供应商配置不存在，请先执行数据库迁移");
        }
        return entity;
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }

    private static int booleanValue(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private static boolean enabled(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
