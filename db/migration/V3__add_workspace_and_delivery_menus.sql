-- 经纬空间 M1：补齐概览与空间、资产与交付菜单及页面访问权限。
-- 当前页面路由已经存在，本迁移只建立可由角色管理分配的菜单资源和稳定权限码。

INSERT INTO jw_sys_permission
    (id, code, name, resource_type, description, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (1041, 'workspace:dashboard:view', '访问工作台总览', 'MENU', '访问平台工作台总览页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1042, 'project:project:list', '查看园区项目', 'MENU', '访问园区项目管理页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1043, 'map:annotation:list', '查看地图标注', 'MENU', '访问地图标注编辑器页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1044, 'building:model:list', '查看楼宇模型', 'MENU', '访问楼宇精细编辑页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1045, 'floor:space:list', '查看楼层空间', 'MENU', '访问楼层与区域管理页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1046, 'asset:resource:list', '查看资源资产', 'MENU', '访问资源资产中心页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (1047, 'release:scene:list', '查看场景发布', 'MENU', '访问草稿与场景发布页面', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

INSERT INTO jw_sys_menu
    (id, parent_id, name, path, component, icon, sort_order, permission_code, visible, created_at, updated_at, created_by, updated_by, deleted, version)
VALUES
    (200, 0, '概览与空间', '/workspace', 'Layout', 'LayoutDashboard', 10, NULL, 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (210, 200, '工作台总览', '/', 'DashboardView', 'LayoutDashboard', 10, 'workspace:dashboard:view', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (220, 200, '园区项目', '/projects', 'ModuleView', 'FolderKanban', 20, 'project:project:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (230, 200, '地图标注编辑器', '/map', 'SpatialWorkspaceView', 'Map', 30, 'map:annotation:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (240, 200, '楼宇精细编辑', '/building', 'ModuleView', 'Building2', 40, 'building:model:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (250, 200, '楼层与区域管理', '/floor', 'ModuleView', 'Layers3', 50, 'floor:space:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (300, 0, '资产与交付', '/delivery', 'Layout', 'Boxes', 20, NULL, 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (310, 300, '资源资产中心', '/assets', 'ModuleView', 'Boxes', 10, 'asset:resource:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0),
    (320, 300, '草稿与场景发布', '/release', 'ModuleView', 'FileClock', 20, 'release:scene:list', 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0, 0);

-- 超级管理员默认拥有新增菜单权限，其他角色由角色管理页面按需勾选。
INSERT INTO jw_sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM jw_sys_permission
WHERE code IN (
    'workspace:dashboard:view',
    'project:project:list',
    'map:annotation:list',
    'building:model:list',
    'floor:space:list',
    'asset:resource:list',
    'release:scene:list'
);
