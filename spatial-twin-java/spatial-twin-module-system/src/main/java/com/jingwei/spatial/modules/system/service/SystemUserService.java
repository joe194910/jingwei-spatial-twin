package com.jingwei.spatial.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.system.dto.PasswordResetRequest;
import com.jingwei.spatial.modules.system.dto.UserSaveRequest;
import com.jingwei.spatial.modules.system.entity.RoleEntity;
import com.jingwei.spatial.modules.system.entity.UserEntity;
import com.jingwei.spatial.modules.system.entity.UserRoleEntity;
import com.jingwei.spatial.modules.system.mapper.RoleMapper;
import com.jingwei.spatial.modules.system.mapper.UserMapper;
import com.jingwei.spatial.modules.system.mapper.UserRoleMapper;
import com.jingwei.spatial.modules.system.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 用户管理服务，支持一个用户关联多个角色并在权限变更后注销旧会话。
 */
@Service
public class SystemUserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemAuditService auditService;

    public SystemUserService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            SystemAuditService auditService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * 查询用户列表，可按登录名或姓名模糊筛选。
     *
     * @param keyword 可选关键词
     * @return 用户列表
     */
    public List<UserVO> list(String keyword) {
        LambdaQueryWrapper<UserEntity> query = Wrappers.<UserEntity>lambdaQuery()
                .orderByAsc(UserEntity::getUsername);
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            query.and(wrapper -> wrapper.like(UserEntity::getUsername, normalized)
                    .or()
                    .like(UserEntity::getNickname, normalized));
        }
        return userMapper.selectList(query).stream().map(this::toVO).toList();
    }

    /**
     * 创建用户并关联多个启用角色。
     *
     * @param request 用户数据
     * @return 新用户标识
     */
    @Transactional
    public long create(UserSaveRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "创建用户时必须设置密码");
        }
        validateStatus(request.status());
        validateRoles(request.roleIds());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UserEntity entity = new UserEntity();
        entity.setUsername(request.username().trim());
        entity.setNickname(request.nickname().trim());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setStatus(request.status());
        entity.setLoginFailCount(0);
        entity.setPasswordChangedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(currentUserId());
        entity.setUpdatedBy(currentUserId());
        entity.setDeleted(0);
        entity.setVersion(0);
        userMapper.insert(entity);
        replaceRoles(entity.getId(), request.roleIds());
        auditService.record("USER", String.valueOf(entity.getId()), "CREATE", "SUCCESS", entity.getUsername());
        return entity.getId();
    }

    /**
     * 修改用户基础信息和多角色关联。
     *
     * @param userId  用户标识
     * @param request 用户数据
     */
    @Transactional
    public void update(long userId, UserSaveRequest request) {
        UserEntity entity = requireUser(userId);
        validateStatus(request.status());
        validateRoles(request.roleIds());
        if (userId == currentUserId() && request.status() == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能禁用当前登录账号");
        }
        entity.setUsername(request.username().trim());
        entity.setNickname(request.nickname().trim());
        entity.setStatus(request.status());
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(currentUserId());
        userMapper.updateById(entity);
        replaceRoles(userId, request.roleIds());
        StpUtil.getStpLogic().logout(userId);
        auditService.record("USER", String.valueOf(userId), "UPDATE", "SUCCESS", entity.getUsername());
    }

    /**
     * 管理员重置用户密码，并使该用户现有会话失效。
     *
     * @param userId  用户标识
     * @param request 新密码
     */
    @Transactional
    public void resetPassword(long userId, PasswordResetRequest request) {
        UserEntity entity = requireUser(userId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setPasswordChangedAt(now);
        entity.setLoginFailCount(0);
        entity.setLockedUntil(null);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(currentUserId());
        userMapper.updateById(entity);
        StpUtil.getStpLogic().logout(userId);
        auditService.record("USER", String.valueOf(userId), "RESET_PASSWORD", "SUCCESS", "管理员重置密码");
    }

    /**
     * 删除普通用户及其角色关联。
     *
     * @param userId 用户标识
     */
    @Transactional
    public void delete(long userId) {
        if (userId == currentUserId()) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能删除当前登录账号");
        }
        UserEntity entity = requireUser(userId);
        userRoleMapper.delete(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getUserId, userId));
        userMapper.deleteById(userId);
        StpUtil.getStpLogic().logout(userId);
        auditService.record("USER", String.valueOf(userId), "DELETE", "SUCCESS", entity.getUsername());
    }

    private UserVO toVO(UserEntity entity) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(entity.getId());
        List<RoleEntity> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds);
        return new UserVO(
                entity.getId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getStatus(),
                entity.getLockedUntil(),
                entity.getLastLoginAt(),
                roleIds,
                roles.stream().map(RoleEntity::getName).toList()
        );
    }

    private void replaceRoles(long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getUserId, userId));
        for (Long roleId : new LinkedHashSet<>(roleIds)) {
            userRoleMapper.insert(new UserRoleEntity(userId, roleId));
        }
    }

    private void validateRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "请至少选择一个角色");
        }
        List<RoleEntity> roles = roleMapper.selectBatchIds(new LinkedHashSet<>(roleIds));
        if (roles.size() != new LinkedHashSet<>(roleIds).size()
                || roles.stream().anyMatch(role -> role.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "所选角色不存在或已停用");
        }
    }

    private UserEntity requireUser(long userId) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在或已删除");
        }
        return entity;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "用户状态只能为0或1");
        }
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }
}
