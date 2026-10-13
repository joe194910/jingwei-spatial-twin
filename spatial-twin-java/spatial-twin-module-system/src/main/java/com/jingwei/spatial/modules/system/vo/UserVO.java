package com.jingwei.spatial.modules.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理列表输出，不包含密码哈希等敏感字段。
 */
public record UserVO(
        long id,
        String username,
        String nickname,
        int status,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        List<Long> roleIds,
        List<String> roleNames
) {
}
