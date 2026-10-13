package com.jingwei.spatial.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingwei.spatial.modules.system.entity.MenuEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MenuMapper extends BaseMapper<MenuEntity> {

    @Select("""
            SELECT m.id
            FROM jw_sys_menu m
            JOIN jw_sys_permission p ON p.code = m.permission_code AND p.deleted = 0
            JOIN jw_sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
              AND m.deleted = 0
              AND m.permission_code IS NOT NULL
              AND m.permission_code <> ''
            ORDER BY m.sort_order, m.id
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
