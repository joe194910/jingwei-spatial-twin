package com.jingwei.spatial.modules.system.service;

import com.jingwei.spatial.modules.system.mapper.RolePermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集中查询用户角色与权限，供 Sa-Token 和登录响应复用。
 */
@Service
public class AccessMetadataService {

    private final RolePermissionMapper rolePermissionMapper;

    public AccessMetadataService(RolePermissionMapper rolePermissionMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * 查询用户当前启用角色的权限并集。
     *
     * @param userId 用户标识
     * @return 稳定权限码列表
     */
    public List<String> getPermissionCodes(long userId) {
        return rolePermissionMapper.selectPermissionCodesByUserId(userId);
    }

    /**
     * 查询用户当前启用的角色编码。
     *
     * @param userId 用户标识
     * @return 角色编码列表
     */
    public List<String> getRoleCodes(long userId) {
        return rolePermissionMapper.selectRoleCodesByUserId(userId);
    }
}
