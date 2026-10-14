# spatial-twin-module-system

系统管理业务模块，负责平台账号、认证、授权和系统治理能力。

## 当前职责

- 登录、退出、当前用户与 Sa-Token 会话
- 用户管理和用户多角色关联
- 角色管理、角色菜单与按钮权限分配
- 菜单及权限资源管理
- 唯一键系统配置管理
- 地图供应商配置、凭证加密和运行时降级参数
- 登录安全策略、操作审计和访问元数据

## 代码位置

- `modules/auth`：登录、退出和当前会话
- `modules/system/controller`：系统管理接口
- `modules/system/service`：事务与业务规则
- `modules/system/mapper`：数据库访问
- `modules/system/entity`：数据库实体
- `modules/system/dto`：接口入参
- `modules/system/vo`：接口出参

## 边界

- 依赖 `spatial-twin-common`，不得依赖 `spatial-twin-server`。
- Controller 不写 SQL，Service 不直接拼接 HTTP 响应。
- 业务接口统一返回 `ApiResponse`，不得直接返回 Entity。
- 权限变更、密码重置、配置变更等关键操作必须经过权限校验并记录审计。
