-- 经纬空间 M1：初始化用户、角色、菜单、权限、配置和审计基础表。
-- MySQL 8.0，所有业务时间按 UTC 写入，主键由应用侧雪花算法生成。

CREATE TABLE jw_sys_user (
    id BIGINT NOT NULL COMMENT '用户主键',
    username VARCHAR(64) NOT NULL COMMENT '登录名',
    password_hash VARCHAR(255) NOT NULL COMMENT 'PBKDF2密码哈希',
    nickname VARCHAR(64) NOT NULL COMMENT '用户姓名',
    mobile_masked VARCHAR(32) NULL COMMENT '脱敏手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until DATETIME(3) NULL COMMENT '账号锁定截止UTC时间',
    last_login_at DATETIME(3) NULL COMMENT '最后登录UTC时间',
    password_changed_at DATETIME(3) NOT NULL COMMENT '密码最后修改UTC时间',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人主键，0表示系统',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人主键，0表示系统',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_jw_sys_user_username (username),
    KEY idx_jw_sys_user_status_deleted (status, deleted),
    KEY idx_jw_sys_user_locked_until (locked_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';

CREATE TABLE jw_sys_role (
    id BIGINT NOT NULL COMMENT '角色主键',
    code VARCHAR(64) NOT NULL COMMENT '稳定角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    description VARCHAR(255) NULL COMMENT '角色说明',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人主键',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人主键',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_jw_sys_role_code (code),
    KEY idx_jw_sys_role_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色';

CREATE TABLE jw_sys_permission (
    id BIGINT NOT NULL COMMENT '权限主键',
    code VARCHAR(128) NOT NULL COMMENT '稳定权限码',
    name VARCHAR(64) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：MENU或BUTTON',
    description VARCHAR(255) NULL COMMENT '权限说明',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人主键',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人主键',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_jw_sys_permission_code (code),
    KEY idx_jw_sys_permission_type_deleted (resource_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限码';

CREATE TABLE jw_sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户主键',
    role_id BIGINT NOT NULL COMMENT '角色主键',
    PRIMARY KEY (user_id, role_id),
    KEY idx_jw_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';

CREATE TABLE jw_sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色主键',
    permission_id BIGINT NOT NULL COMMENT '权限主键',
    PRIMARY KEY (role_id, permission_id),
    KEY idx_jw_sys_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联';

CREATE TABLE jw_sys_menu (
    id BIGINT NOT NULL COMMENT '菜单主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单主键，0表示根节点',
    name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    path VARCHAR(160) NOT NULL COMMENT '前端路由路径或按钮标识',
    component VARCHAR(160) NULL COMMENT '前端组件标识',
    icon VARCHAR(64) NULL COMMENT 'Lucide图标名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序号',
    permission_code VARCHAR(128) NULL COMMENT '关联稳定权限码',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否在导航可见：0否，1是',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人主键',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人主键',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    KEY idx_jw_sys_menu_parent_sort (parent_id, sort_order),
    KEY idx_jw_sys_menu_permission_code (permission_code),
    KEY idx_jw_sys_menu_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台菜单树';

CREATE TABLE jw_sys_setting (
    id BIGINT NOT NULL COMMENT '配置主键',
    setting_key VARCHAR(128) NOT NULL COMMENT '全局唯一配置键',
    setting_value VARCHAR(512) NOT NULL COMMENT '配置值',
    value_type VARCHAR(32) NOT NULL COMMENT '值类型：INTEGER或BOOLEAN',
    description VARCHAR(255) NOT NULL COMMENT '中文配置说明',
    sensitive_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否敏感：0否，1是',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人主键',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人主键',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_jw_sys_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统唯一配置';

CREATE TABLE jw_sys_audit_log (
    id BIGINT NOT NULL COMMENT '审计日志主键',
    user_id BIGINT NULL COMMENT '操作用户主键，未登录时为空',
    object_type VARCHAR(64) NOT NULL COMMENT '业务对象类型',
    object_id VARCHAR(128) NULL COMMENT '业务对象标识',
    action VARCHAR(64) NOT NULL COMMENT '操作动作',
    result VARCHAR(32) NOT NULL COMMENT '操作结果',
    request_id VARCHAR(64) NULL COMMENT '请求追踪标识',
    ip VARCHAR(64) NULL COMMENT '来源IP',
    occurred_at DATETIME(3) NOT NULL COMMENT '发生UTC时间',
    details_masked VARCHAR(1000) NULL COMMENT '脱敏操作摘要',
    PRIMARY KEY (id),
    KEY idx_jw_sys_audit_log_occurred_at (occurred_at),
    KEY idx_jw_sys_audit_log_user_time (user_id, occurred_at),
    KEY idx_jw_sys_audit_log_object (object_type, object_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全审计日志';

CREATE TABLE jw_sys_login_log (
    id BIGINT NOT NULL COMMENT '登录日志主键',
    user_id BIGINT NULL COMMENT '匹配到的用户主键',
    username_snapshot VARCHAR(64) NOT NULL COMMENT '登录名快照',
    ip VARCHAR(64) NULL COMMENT '来源IP',
    result VARCHAR(32) NOT NULL COMMENT '登录结果',
    reason VARCHAR(255) NULL COMMENT '失败原因或成功摘要',
    request_id VARCHAR(64) NULL COMMENT '请求追踪标识',
    occurred_at DATETIME(3) NOT NULL COMMENT '发生UTC时间',
    PRIMARY KEY (id),
    KEY idx_jw_sys_login_log_user_time (user_id, occurred_at),
    KEY idx_jw_sys_login_log_username_time (username_snapshot, occurred_at),
    KEY idx_jw_sys_login_log_occurred_at (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录安全日志';

-- 预置超级管理员角色。首个管理员账号由受控环境变量创建，不在SQL中保存默认密码。
INSERT INTO jw_sys_role
    (id, code, name, status, description, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (1, 'SUPER_ADMIN', '超级管理员', 1, '平台最高管理角色，不可删除或停用', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

INSERT INTO jw_sys_permission
    (id, code, name, resource_type, description, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (1001, 'system:user:list', '查看用户', 'MENU', '查看用户列表', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1002, 'system:user:add', '新增用户', 'BUTTON', '创建用户并分配角色', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1003, 'system:user:edit', '编辑用户', 'BUTTON', '修改用户、角色或重置密码', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1004, 'system:user:delete', '删除用户', 'BUTTON', '逻辑删除普通用户', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1011, 'system:role:list', '查看角色', 'MENU', '查看角色列表与菜单授权', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1012, 'system:role:add', '新增角色', 'BUTTON', '创建角色', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1013, 'system:role:edit', '编辑角色', 'BUTTON', '修改角色并勾选菜单', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1014, 'system:role:delete', '删除角色', 'BUTTON', '删除未关联用户的角色', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1021, 'system:menu:list', '查看菜单', 'MENU', '查看菜单树', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1022, 'system:menu:add', '新增菜单', 'BUTTON', '创建菜单或按钮权限节点', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1023, 'system:menu:edit', '编辑菜单', 'BUTTON', '修改菜单或权限码', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1024, 'system:menu:delete', '删除菜单', 'BUTTON', '删除无子节点菜单', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1031, 'system:setting:list', '查看系统配置', 'MENU', '超级管理员查看系统唯一配置', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1032, 'system:setting:edit', '编辑系统配置', 'BUTTON', '超级管理员修改系统唯一配置', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

INSERT INTO jw_sys_menu
    (id, parent_id, name, path, component, icon, sort_order, permission_code, visible, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (100, 0, '系统管理', '/system', 'Layout', 'Settings', 100, NULL, 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (110, 100, '用户管理', '/system/users', 'system/UserManagementView', 'Users', 110, 'system:user:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (111, 110, '新增用户', '#user-add', NULL, NULL, 111, 'system:user:add', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (112, 110, '编辑用户', '#user-edit', NULL, NULL, 112, 'system:user:edit', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (113, 110, '删除用户', '#user-delete', NULL, NULL, 113, 'system:user:delete', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (120, 100, '角色管理', '/system/roles', 'system/RoleManagementView', 'ShieldCheck', 120, 'system:role:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (121, 120, '新增角色', '#role-add', NULL, NULL, 121, 'system:role:add', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (122, 120, '编辑角色', '#role-edit', NULL, NULL, 122, 'system:role:edit', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (123, 120, '删除角色', '#role-delete', NULL, NULL, 123, 'system:role:delete', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (130, 100, '菜单管理', '/system/menus', 'system/MenuManagementView', 'Menu', 130, 'system:menu:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (131, 130, '新增菜单', '#menu-add', NULL, NULL, 131, 'system:menu:add', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (132, 130, '编辑菜单', '#menu-edit', NULL, NULL, 132, 'system:menu:edit', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (133, 130, '删除菜单', '#menu-delete', NULL, NULL, 133, 'system:menu:delete', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (140, 100, '系统配置', '/system/settings', 'system/SystemSettingView', 'SlidersHorizontal', 140, 'system:setting:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (141, 140, '编辑系统配置', '#setting-edit', NULL, NULL, 141, 'system:setting:edit', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

INSERT INTO jw_sys_role_permission (role_id, permission_id)
SELECT 1, id FROM jw_sys_permission WHERE deleted = 0;

INSERT INTO jw_sys_setting
    (id, setting_key, setting_value, value_type, description, sensitive_flag, created_at, updated_at, created_by, updated_by, version)
VALUES
    (2001, 'security.login.max-failures', '5', 'INTEGER', '连续登录失败锁定阈值，允许3至20次', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0),
    (2002, 'security.login.lock-minutes', '30', 'INTEGER', '达到失败阈值后的锁定分钟数，允许1至1440分钟', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0),
    (2003, 'security.password.min-length', '12', 'INTEGER', '新密码最小长度，允许12至128位', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0),
    (2004, 'system.registration.enabled', 'false', 'BOOLEAN', '是否开放自助注册，默认关闭', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0);
