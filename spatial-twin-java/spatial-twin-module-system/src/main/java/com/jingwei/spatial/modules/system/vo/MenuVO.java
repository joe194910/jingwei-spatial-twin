package com.jingwei.spatial.modules.system.vo;

import java.util.List;

/**
 * 菜单树输出。
 */
public record MenuVO(
        long id,
        long parentId,
        String name,
        String path,
        String component,
        String icon,
        int sortOrder,
        String permissionCode,
        int visible,
        List<MenuVO> children
) {
}
