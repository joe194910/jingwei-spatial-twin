-- 经纬空间 M2：补充园区项目逻辑删除权限。
-- 删除仅隐藏项目业务数据，审计记录继续保留，便于后续追溯。

INSERT INTO jw_sys_permission
    (id, code, name, resource_type, description, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (1054, 'project:project:delete', '删除园区项目', 'BUTTON', '逻辑删除园区项目并保留审计记录', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

INSERT INTO jw_sys_menu
    (id, parent_id, name, path, component, icon, sort_order, permission_code, visible, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (224, 220, '删除园区项目', '#project-delete', NULL, NULL, 24, 'project:project:delete', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

-- 超级管理员默认拥有删除权限，其他角色在角色管理中按需分配。
INSERT INTO jw_sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM jw_sys_permission
WHERE code = 'project:project:delete';
