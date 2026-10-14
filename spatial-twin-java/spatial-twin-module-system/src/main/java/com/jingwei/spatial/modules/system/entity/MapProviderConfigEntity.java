package com.jingwei.spatial.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 地图供应商单例配置实体，凭证字段只保存密文。
 */
@TableName("jw_map_provider_config")
public class MapProviderConfigEntity {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String providerCode;
    private Integer enabled;
    private String apiVersion;
    private String webKeyCiphertext;
    private String securityJsCodeCiphertext;
    private String mapStyle;
    private String viewMode;
    private Integer defaultPitch;
    private Integer defaultRotation;
    private Integer minZoom;
    private Integer maxZoom;
    private Integer showScale;
    private Integer showToolbar;
    private Integer showControlBar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    @Version
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getWebKeyCiphertext() {
        return webKeyCiphertext;
    }

    public void setWebKeyCiphertext(String webKeyCiphertext) {
        this.webKeyCiphertext = webKeyCiphertext;
    }

    public String getSecurityJsCodeCiphertext() {
        return securityJsCodeCiphertext;
    }

    public void setSecurityJsCodeCiphertext(String securityJsCodeCiphertext) {
        this.securityJsCodeCiphertext = securityJsCodeCiphertext;
    }

    public String getMapStyle() {
        return mapStyle;
    }

    public void setMapStyle(String mapStyle) {
        this.mapStyle = mapStyle;
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    public Integer getDefaultPitch() {
        return defaultPitch;
    }

    public void setDefaultPitch(Integer defaultPitch) {
        this.defaultPitch = defaultPitch;
    }

    public Integer getDefaultRotation() {
        return defaultRotation;
    }

    public void setDefaultRotation(Integer defaultRotation) {
        this.defaultRotation = defaultRotation;
    }

    public Integer getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(Integer minZoom) {
        this.minZoom = minZoom;
    }

    public Integer getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(Integer maxZoom) {
        this.maxZoom = maxZoom;
    }

    public Integer getShowScale() {
        return showScale;
    }

    public void setShowScale(Integer showScale) {
        this.showScale = showScale;
    }

    public Integer getShowToolbar() {
        return showToolbar;
    }

    public void setShowToolbar(Integer showToolbar) {
        this.showToolbar = showToolbar;
    }

    public Integer getShowControlBar() {
        return showControlBar;
    }

    public void setShowControlBar(Integer showControlBar) {
        this.showControlBar = showControlBar;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
