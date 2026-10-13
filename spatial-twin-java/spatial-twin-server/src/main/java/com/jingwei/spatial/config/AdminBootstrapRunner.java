package com.jingwei.spatial.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingwei.spatial.modules.system.entity.RoleEntity;
import com.jingwei.spatial.modules.system.entity.UserEntity;
import com.jingwei.spatial.modules.system.entity.UserRoleEntity;
import com.jingwei.spatial.modules.system.mapper.RoleMapper;
import com.jingwei.spatial.modules.system.mapper.UserMapper;
import com.jingwei.spatial.modules.system.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 显式启用时创建首个超级管理员，密码只从环境变量读取且不会输出到日志。
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final boolean enabled;
    private final String username;
    private final String password;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            @Value("${jingwei.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${jingwei.bootstrap-admin.username:admin}") String username,
            @Value("${jingwei.bootstrap-admin.password:}") String password,
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("已启用首个管理员初始化，但 JW_BOOTSTRAP_ADMIN_PASSWORD 未设置或少于12位");
        }
        UserEntity existing = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username));
        if (existing != null) {
            log.info("首个管理员账号已存在，跳过初始化");
            return;
        }
        RoleEntity superAdmin = roleMapper.selectOne(Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getCode, "SUPER_ADMIN"));
        if (superAdmin == null) {
            throw new IllegalStateException("数据库缺少 SUPER_ADMIN 初始角色，请检查 Flyway 迁移");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UserEntity user = new UserEntity();
        user.setUsername(username.trim());
        user.setNickname("系统管理员");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setPasswordChangedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setCreatedBy(0L);
        user.setUpdatedBy(0L);
        user.setDeleted(0);
        user.setVersion(0);
        userMapper.insert(user);
        userRoleMapper.insert(new UserRoleEntity(user.getId(), superAdmin.getId()));
        log.info("首个超级管理员已完成初始化，登录名为 {}", username);
    }
}
