package com.jingwei.spatial.modules.auth.vo;

import java.util.List;

/**
 * 登录成功后返回的短期会话信息。
 */
public record LoginVO(
        String tokenName,
        String tokenValue,
        long userId,
        String username,
        String nickname,
        List<String> roles,
        List<String> permissions
) {
}
