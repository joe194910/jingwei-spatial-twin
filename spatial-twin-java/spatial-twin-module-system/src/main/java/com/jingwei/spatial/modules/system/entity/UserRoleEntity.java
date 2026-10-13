package com.jingwei.spatial.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户与角色的多对多关联。
 */
@TableName("jw_sys_user_role")
public class UserRoleEntity {

    private Long userId;
    private Long roleId;

    public UserRoleEntity() {
    }

    public UserRoleEntity(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
