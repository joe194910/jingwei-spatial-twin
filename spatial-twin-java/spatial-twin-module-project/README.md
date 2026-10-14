# spatial-twin-module-project

项目业务模块，负责园区项目、项目成员范围和默认地图启动参数。

## 当前职责

- 项目分页查询、详情、创建、修改和归档
- 项目创建人自动成为项目负责人
- 超级管理员与项目成员的数据范围校验
- 默认地图中心、缩放、俯仰、旋转和二维/三维模式配置
- 项目关键操作审计

## 代码位置

- `modules/project/controller`：项目与默认地图接口
- `modules/project/service`：项目事务、数据权限和业务规则
- `modules/project/mapper`：MyBatis-Plus 数据访问
- `modules/project/entity`：项目、成员和启动参数数据库实体
- `modules/project/dto`：创建、修改和启动参数入参
- `modules/project/vo`：项目与启动参数出参

## 依赖方向

- 仅依赖 `spatial-twin-common` 和基础框架。
- 审计通过 common 中的 `AuditRecorder` 端口调用，不依赖系统治理模块实现。
- 由 `spatial-twin-server` 统一装配项目模块和系统治理模块。

## 禁止放置

- 不放用户、角色、菜单等系统治理实体。
- 不放地图厂商 SDK 实现、Three.js 场景对象或文件资产处理逻辑。
- 不在 Controller 拼接查询或绕过项目成员范围校验。
