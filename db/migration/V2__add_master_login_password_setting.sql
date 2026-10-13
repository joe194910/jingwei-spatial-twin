-- 经纬空间 M1：增加管理员可维护的万能登录密码配置。
-- 配置值只保存 PBKDF2 哈希；初始值“!”表示未启用，必须由超级管理员在系统配置页设置。

INSERT INTO jw_sys_setting
    (id, setting_key, setting_value, value_type, description, sensitive_flag, created_at, updated_at, created_by, updated_by, version)
VALUES
    (2005, 'security.login.master-password', '!', 'PASSWORD', '管理员万能登录密码，设置后可用于登录任一启用账号，仅保存哈希且不回显', 1,
     UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), 0, 0, 0);
