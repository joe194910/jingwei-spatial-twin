package com.jingwei.spatial.modules.system.vo;

/**
 * 系统单值配置输出，敏感配置不会返回明文。
 */
public record SettingVO(
        String key,
        String value,
        String valueType,
        String description,
        boolean sensitive
) {
}
