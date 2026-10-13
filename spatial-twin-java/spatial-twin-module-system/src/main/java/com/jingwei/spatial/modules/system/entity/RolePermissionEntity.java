package com.jingwei.spatial.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色与权限码的多对多关联。
 */
@TableName("jw_sys_role_permission")
public class RolePermissionEntity {

    private Long roleId;
    private Long permissionId;

    public RolePermissionEntity() {
    }

    public RolePermissionEntity(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
