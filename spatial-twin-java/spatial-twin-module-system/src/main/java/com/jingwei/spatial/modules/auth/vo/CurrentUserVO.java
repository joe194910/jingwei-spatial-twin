package com.jingwei.spatial.modules.auth.vo;

import java.util.List;

/**
 * 当前登录用户与有效权限集合。
 */
public record CurrentUserVO(
        long userId,
        String username,
        String nickname,
        List<String> roles,
        List<String> permissions
) {
}
