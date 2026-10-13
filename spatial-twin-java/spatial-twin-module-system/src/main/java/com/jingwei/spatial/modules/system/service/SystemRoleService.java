package com.jingwei.spatial.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.system.dto.RoleSaveRequest;
import com.jingwei.spatial.modules.system.entity.MenuEntity;
import com.jingwei.spatial.modules.system.entity.PermissionEntity;
import com.jingwei.spatial.modules.system.entity.RoleEntity;
import com.jingwei.spatial.modules.system.entity.RolePermissionEntity;
import com.jingwei.spatial.modules.system.mapper.MenuMapper;
import com.jingwei.spatial.modules.system.mapper.PermissionMapper;
import com.jingwei.spatial.modules.system.mapper.RoleMapper;
import com.jingwei.spatial.modules.system.mapper.RolePermissionMapper;
import com.jingwei.spatial.modules.system.mapper.UserRoleMapper;
import com.jingwei.spatial.modules.system.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色管理服务，负责角色基础信息、菜单权限勾选和会话即时失效。
 */
@Service
public class SystemRoleService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;
    private final SystemAuditService auditService;

    public SystemRoleService(
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            RolePermissionMapper rolePermissionMapper,
            MenuMapper menuMapper,
            PermissionMapper permissionMapper,
            SystemAuditService auditService
    ) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.auditService = auditService;
    }

    /**
     * 查询角色及其已勾选菜单。
     *
     * @return 角色列表
     */
    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<RoleEntity>lambdaQuery()
                        .orderByAsc(RoleEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 创建角色并保存菜单权限。
     *
     * @param request 角色数据
     * @return 新角色标识
     */
    @Transactional
    public long create(RoleSaveRequest request) {
        validateStatus(request.status());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        RoleEntity entity = new RoleEntity();
        apply(entity, request);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(currentUserId());
        entity.setUpdatedBy(currentUserId());
        entity.setDeleted(0);
        entity.setVersion(0);
        roleMapper.insert(entity);
        replacePermissions(entity.getId(), request.menuIds());
        auditService.record("ROLE", String.valueOf(entity.getId()), "CREATE", "SUCCESS", entity.getCode());
        return entity.getId();
    }

    /**
     * 修改角色及菜单权限，变更后注销该角色用户的旧会话。
     *
     * @param roleId  角色标识
     * @param request 角色数据
     */
    @Transactional
    public void update(long roleId, RoleSaveRequest request) {
        RoleEntity entity = requireRole(roleId);
        validateStatus(request.status());
        if (SUPER_ADMIN.equals(entity.getCode())
                && (!SUPER_ADMIN.equals(request.code()) || request.status() != 1)) {
            throw new BusinessException(ErrorCode.CONFLICT, "超级管理员角色编码和启用状态不可修改");
        }
        apply(entity, request);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(currentUserId());
        roleMapper.updateById(entity);
        replacePermissions(roleId, request.menuIds());
        invalidateRoleSessions(roleId);
        auditService.record("ROLE", String.valueOf(roleId), "UPDATE", "SUCCESS", entity.getCode());
    }

    /**
     * 删除未分配用户的普通角色。
     *
     * @param roleId 角色标识
     */
    @Transactional
    public void delete(long roleId) {
        RoleEntity entity = requireRole(roleId);
        if (SUPER_ADMIN.equals(entity.getCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "超级管理员角色不可删除");
        }
        if (userRoleMapper.countUsersByRoleId(roleId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该角色仍有关联用户，无法删除");
        }
        rolePermissionMapper.delete(Wrappers.<RolePermissionEntity>lambdaQuery()
                .eq(RolePermissionEntity::getRoleId, roleId));
        roleMapper.deleteById(roleId);
        auditService.record("ROLE", String.valueOf(roleId), "DELETE", "SUCCESS", entity.getCode());
    }

    private void replacePermissions(long roleId, List<Long> menuIds) {
        rolePermissionMapper.delete(Wrappers.<RolePermissionEntity>lambdaQuery()
                .eq(RolePermissionEntity::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<MenuEntity> menus = menuMapper.selectBatchIds(new LinkedHashSet<>(menuIds));
        Set<String> permissionCodes = menus.stream()
                .map(MenuEntity::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (permissionCodes.isEmpty()) {
            return;
        }
        List<PermissionEntity> permissions = permissionMapper.selectList(Wrappers.<PermissionEntity>lambdaQuery()
                .in(PermissionEntity::getCode, permissionCodes));
        for (PermissionEntity permission : permissions) {
            rolePermissionMapper.insert(new RolePermissionEntity(roleId, permission.getId()));
        }
    }

    private void invalidateRoleSessions(long roleId) {
        for (Long userId : userRoleMapper.selectUserIdsByRoleId(roleId)) {
            StpUtil.getStpLogic().logout(userId);
        }
    }

    private RoleVO toVO(RoleEntity entity) {
        return new RoleVO(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getStatus(),
                entity.getDescription(),
                menuMapper.selectMenuIdsByRoleId(entity.getId()),
                userRoleMapper.countUsersByRoleId(entity.getId())
        );
    }

    private RoleEntity requireRole(long roleId) {
        RoleEntity entity = roleMapper.selectById(roleId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在或已删除");
        }
        return entity;
    }

    private void apply(RoleEntity entity, RoleSaveRequest request) {
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setStatus(request.status());
        entity.setDescription(request.description() == null ? null : request.description().trim());
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色状态只能为0或1");
        }
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }
}
