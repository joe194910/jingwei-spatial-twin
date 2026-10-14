-- 经纬空间 M1：统一管理后台根菜单名称。
-- 左侧导航已改为读取菜单表，后续可直接在“菜单管理”中修改名称、排序、图标和显隐。

UPDATE jw_sys_menu
SET name = '管理后台',
    updated_at = UTC_TIMESTAMP(3),
    version = version + 1
WHERE id = 100
  AND deleted = 0
  AND name = '系统管理';
