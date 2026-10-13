package com.jingwei.spatial.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingwei.spatial.modules.system.entity.RolePermissionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {

    @Select("""
            SELECT DISTINCT p.code
            FROM jw_sys_user_role ur
            JOIN jw_sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
            JOIN jw_sys_role_permission rp ON rp.role_id = r.id
            JOIN jw_sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
            WHERE ur.user_id = #{userId}
            ORDER BY p.code
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT r.code
            FROM jw_sys_user_role ur
            JOIN jw_sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
            WHERE ur.user_id = #{userId}
            ORDER BY r.code
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT permission_id
            FROM jw_sys_role_permission
            WHERE role_id = #{roleId}
            ORDER BY permission_id
            """)
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
