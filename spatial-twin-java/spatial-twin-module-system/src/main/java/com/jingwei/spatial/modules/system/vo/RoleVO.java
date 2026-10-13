package com.jingwei.spatial.modules.system.vo;

import java.util.List;

/**
 * 角色管理输出，包含角色已勾选的菜单节点。
 */
public record RoleVO(
        long id,
        String code,
        String name,
        int status,
        String description,
        List<Long> menuIds,
        int userCount
) {
}
