package com.jingwei.spatial.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingwei.spatial.modules.system.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {

    @Select("""
            SELECT role_id
            FROM jw_sys_user_role
            WHERE user_id = #{userId}
            ORDER BY role_id
            """)
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM jw_sys_user_role
            WHERE role_id = #{roleId}
            """)
    int countUsersByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT user_id
            FROM jw_sys_user_role
            WHERE role_id = #{roleId}
            ORDER BY user_id
            """)
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
