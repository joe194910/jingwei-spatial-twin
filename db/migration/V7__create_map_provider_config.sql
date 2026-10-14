-- 经纬空间 M2：建立统一地图供应商配置。
-- 高德凭证只保存 AES-GCM 密文；加密主密钥由部署环境变量提供，不写入数据库或源码。

CREATE TABLE jw_map_provider_config (
    id BIGINT NOT NULL COMMENT '配置主键，当前固定为1',
    provider_code VARCHAR(32) NOT NULL COMMENT '地图供应商编码，当前支持AMAP',
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用在线地图：0否，1是',
    api_version VARCHAR(16) NOT NULL DEFAULT '2.0' COMMENT '高德JS API版本',
    web_key_ciphertext VARCHAR(512) NULL COMMENT 'Web端Key的AES-GCM密文',
    security_js_code_ciphertext VARCHAR(512) NULL COMMENT '安全密钥的AES-GCM密文',
    map_style VARCHAR(160) NULL COMMENT '高德地图样式标识，空值使用供应商默认样式',
    view_mode VARCHAR(8) NOT NULL DEFAULT '3D' COMMENT '地图视图模式：2D或3D',
    default_pitch INT NOT NULL DEFAULT 45 COMMENT '默认俯仰角，范围0至83度',
    default_rotation INT NOT NULL DEFAULT 0 COMMENT '默认旋转角，范围0至359度',
    min_zoom INT NOT NULL DEFAULT 3 COMMENT '最小缩放级别',
    max_zoom INT NOT NULL DEFAULT 20 COMMENT '最大缩放级别',
    show_scale TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示比例尺：0否，1是',
    show_toolbar TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示缩放工具：0否，1是',
    show_control_bar TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示旋转与倾斜控件：0否，1是',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人主键，0表示系统',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人主键，0表示系统',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    CONSTRAINT ck_jw_map_provider_config_singleton CHECK (id = 1),
    CONSTRAINT ck_jw_map_provider_config_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT ck_jw_map_provider_config_view_mode CHECK (view_mode IN ('2D', '3D')),
    CONSTRAINT ck_jw_map_provider_config_pitch CHECK (default_pitch BETWEEN 0 AND 83),
    CONSTRAINT ck_jw_map_provider_config_rotation CHECK (default_rotation BETWEEN 0 AND 359),
    CONSTRAINT ck_jw_map_provider_config_zoom CHECK (
        min_zoom BETWEEN 2 AND 20
        AND max_zoom BETWEEN 2 AND 20
        AND min_zoom <= max_zoom
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地图供应商运行配置';

INSERT INTO jw_map_provider_config
    (id, provider_code, enabled, api_version, web_key_ciphertext, security_js_code_ciphertext,
     map_style, view_mode, default_pitch, default_rotation, min_zoom, max_zoom,
     show_scale, show_toolbar, show_control_bar,
     created_at, updated_at, created_by, updated_by, version)
VALUES
    (1, 'AMAP', 0, '2.0', NULL, NULL,
     NULL, '3D', 45, 0, 3, 20,
     1, 1, 1,
     UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0);
