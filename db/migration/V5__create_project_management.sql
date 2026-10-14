-- 经纬空间 M2：建立园区项目、项目成员范围和默认地图启动参数。
-- 项目创建人自动成为负责人；坐标必须由用户录入，不预置未经校准的工程位置。

ALTER TABLE jw_sys_audit_log
    ADD COLUMN project_id BIGINT NULL COMMENT '所属项目主键，无项目归属时为空' AFTER user_id,
    ADD COLUMN user_agent VARCHAR(255) NULL COMMENT '来源浏览器标识，最长保留255位' AFTER ip,
    ADD KEY idx_jw_sys_audit_log_project_time (project_id, occurred_at);

CREATE TABLE jw_project (
    id BIGINT NOT NULL COMMENT '项目主键',
    project_code VARCHAR(64) NOT NULL COMMENT '稳定项目编码',
    name VARCHAR(128) NOT NULL COMMENT '项目名称',
    description VARCHAR(500) NULL COMMENT '项目说明',
    location_name VARCHAR(255) NOT NULL COMMENT '项目位置名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE进行中，ARCHIVED已归档',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_jw_project_code (project_code),
    KEY idx_jw_project_status_updated (status, updated_at),
    KEY idx_jw_project_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='园区项目';

CREATE TABLE jw_project_startup (
    project_id BIGINT NOT NULL COMMENT '项目主键',
    map_provider VARCHAR(32) NOT NULL DEFAULT 'AMAP' COMMENT '地图适配器：当前为AMAP',
    map_mode VARCHAR(16) NOT NULL DEFAULT '2D' COMMENT '地图模式：2D或3D',
    center_lng DECIMAL(10,7) NOT NULL COMMENT '默认中心经度',
    center_lat DECIMAL(10,7) NOT NULL COMMENT '默认中心纬度',
    zoom_level DECIMAL(5,2) NOT NULL COMMENT '默认缩放级别',
    pitch DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '默认俯仰角',
    rotation DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '默认旋转角',
    created_at DATETIME(3) NOT NULL COMMENT '创建UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新UTC时间',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目默认地图启动参数';

CREATE TABLE jw_project_member (
    project_id BIGINT NOT NULL COMMENT '项目主键',
    user_id BIGINT NOT NULL COMMENT '成员用户主键',
    project_role VARCHAR(16) NOT NULL COMMENT '项目角色：OWNER、MANAGER或VIEWER',
    created_at DATETIME(3) NOT NULL COMMENT '加入项目UTC时间',
    created_by BIGINT NOT NULL COMMENT '操作人主键',
    PRIMARY KEY (project_id, user_id),
    KEY idx_jw_project_member_user (user_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成员与对象访问范围';

INSERT INTO jw_sys_permission
    (id, code, name, resource_type, description, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (1051, 'project:project:add', '新增园区项目', 'BUTTON', '创建项目并设置默认地图参数', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1052, 'project:project:edit', '编辑园区项目', 'BUTTON', '修改项目资料和默认地图参数', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1053, 'project:project:archive', '归档园区项目', 'BUTTON', '将进行中的项目归档为只读状态', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

INSERT INTO jw_sys_menu
    (id, parent_id, name, path, component, icon, sort_order, permission_code, visible, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (221, 220, '新增园区项目', '#project-add', NULL, NULL, 21, 'project:project:add', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (222, 220, '编辑园区项目', '#project-edit', NULL, NULL, 22, 'project:project:edit', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (223, 220, '归档园区项目', '#project-archive', NULL, NULL, 23, 'project:project:archive', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

UPDATE jw_sys_menu
SET component = 'project/ProjectManagementView',
    updated_at = UTC_TIMESTAMP(3),
    updated_by = 0,
    version = version + 1
WHERE id = 220
  AND deleted = 0;

-- 超级管理员默认拥有项目操作权限，其他角色后续在角色管理中按需分配。
INSERT INTO jw_sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM jw_sys_permission
WHERE code IN (
    'project:project:add',
    'project:project:edit',
    'project:project:archive'
);
