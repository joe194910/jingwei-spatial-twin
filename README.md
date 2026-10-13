# 经纬空间 · 数字孪生管理平台

英文名称：**Jingwei Spatial Twin**

经纬空间是面向园区、楼宇、物业和工厂场景的轻量级空间数字孪生管理平台。项目按 `M0` 至 `M7` 逐阶段交付，当前处于 **M1 注册登录与 RBAC 联调阶段**。

## 当前进度

- 已提供产品、架构、数据、安全和开发规范总纲。
- 已提供 12 个页面的独立 HTML 交互原型。
- 已在 `spatial-twin-web-admin` 中完成真实登录、退出、当前用户、用户、角色、菜单和系统配置接口对接。
- 已在 `spatial-twin-java` 中创建 Spring Boot 3 模块化后端，包含统一响应、登录、RBAC、系统配置与安全审计基础能力。
- 已在根目录 `db/migration` 中集中管理 MySQL 8.0 Flyway 迁移脚本。

详细任务拆分见 [`docs/00-overview/DEVELOPMENT_ROADMAP.md`](docs/00-overview/DEVELOPMENT_ROADMAP.md)。

当前正在进行的任务、未完成改动和下一轮接续顺序见
[`docs/00-overview/CURRENT_HANDOFF.md`](docs/00-overview/CURRENT_HANDOFF.md)。

## 目录说明

```text
spatial-twin-web-admin/                 Vue 3 + TypeScript 管理端工程
spatial-twin-java/                      Java 21 + Spring Boot 3 后端工程
  spatial-twin-common/                  统一响应、异常与请求上下文
  spatial-twin-module-system/           认证、用户、角色、菜单、配置与审计
  spatial-twin-server/                  Spring Boot 启动、配置与模块装配
db/migration/                           MySQL 8.0 Flyway 迁移脚本
docs/00-overview/                       公共阶段计划与项目概览
JINGWEI_SPATIAL_TWIN_MASTER_PLAN.md     公共项目开发总规划
AGENTS.md                               公共 AI 开发约束
jingwei-ui-prototype.html               公共原始单文件交互原型
```

根目录承载前后端共享的规范、规划、原型、说明和数据库迁移脚本；依赖、构建产物、运行配置及业务代码归属对应的前端或后端目录。

## 前端开发环境

- Node.js：当前验证版本 `24.18.0`
- pnpm：当前验证版本 `11.14.0`
- 浏览器：现代 Chrome 或 Edge

安装依赖并启动：

```powershell
cd spatial-twin-web-admin
pnpm.cmd install
pnpm.cmd dev
```

访问地址：`http://localhost:5173`

执行最小检查：

```powershell
cd spatial-twin-web-admin
pnpm.cmd type-check
pnpm.cmd build
```

## 后端环境基线

- JDK 21
- Maven 3.9+
- Spring Boot 3.x MVC
- MySQL 8.0
- Redis 7

当前机器已安装 JDK 21。若终端默认 `JAVA_HOME` 仍指向旧版本，请在执行 Maven 前切换到 JDK 21；项目不会用较低 Java 版本替代总纲要求。

## 配置与安全

1. 前端使用 Vite 环境变量文件，后端使用 `spatial-twin-java/spatial-twin-server/src/main/resources` 下的 YAML 与系统环境变量。
2. 不得提交数据库密码、高德 Key、Token 或其他密钥。
3. 当前页面中的园区、建筑、人员数量和性能指标均为原型模拟数据，不代表真实工程成果。
4. “按三级等保目标设计”不代表系统已经通过等级保护测评。

## 原型

`jingwei-ui-prototype.html` 可以直接在浏览器中打开。它只演示界面和交互，不请求真实高德地图、后端接口或三维模型。
