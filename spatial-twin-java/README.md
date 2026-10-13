# Jingwei Spatial Twin Backend

经纬空间数字孪生管理平台后端，采用 Java 21、Spring Boot 3 MVC、Sa-Token、MyBatis-Plus、MySQL 8、Redis、Hutool 与 YAML。

后端是单体模块化 Maven 工程，不拆微服务。当前只有三个职责明确的子模块：

```text
spatial-twin-server
        |
        v
spatial-twin-module-system
        |
        v
spatial-twin-common
```

| 模块 | 一句话说明 | 主要内容 |
|---|---|---|
| `spatial-twin-common` | 跨业务公共基础 | 统一响应体、错误码、全局异常、请求标识 |
| `spatial-twin-module-system` | 系统治理业务 | 登录、用户、角色、菜单、权限、系统配置、审计 |
| `spatial-twin-server` | 可执行应用装配 | 启动类、YAML、框架配置、MinIO、健康检查、模块汇总 |

## 代码放在哪里

| 要开发的内容 | 归属 |
|---|---|
| 通用响应、异常、请求上下文 | `spatial-twin-common` |
| 登录、账号、角色、菜单、配置、审计 | `spatial-twin-module-system` |
| Spring Bean 装配、拦截器、启动任务、运行配置 | `spatial-twin-server` |
| 项目、地图、楼宇等后续业务 | 新建 `spatial-twin-module-project`、`spatial-twin-module-spatial`、`spatial-twin-module-building` 等业务模块 |
| 表结构和必要基础数据变更 | 根目录 `../db/migration` |
| 业务查询 SQL | 对应业务模块的 Mapper Java/XML |

认证不再单独建立 `auth` Maven 模块。它与用户、角色、权限强关联，代码保留在系统模块的 `modules/auth` 包中，既能区分代码职责，也避免无意义的模块依赖。

## 常用命令

MySQL 中只创建空数据库，表结构和基础数据由 Flyway 自动迁移：

```sql
CREATE DATABASE IF NOT EXISTS jingwei_spatial
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

```powershell
# 编译全部后端模块
mvn clean compile

# 编译系统业务模块及其依赖
mvn -pl spatial-twin-module-system -am compile

# 启动完整后端
mvn -pl spatial-twin-server -am spring-boot:run
```

当前工程要求 JDK 21、Maven 3.9+、MySQL 8.0、Redis 7 和 MinIO。开发环境连接参数统一维护在 `application-dev.yml`，生产环境密钥只通过环境变量注入。
不要在数据库客户端中重复执行 `../db/migration/V1__initialize_rbac.sql`；启动后端时 Flyway 会自动执行尚未执行的迁移。遇到表已存在错误时，按 [`../db/README.md`](../db/README.md) 的迁移冲突流程处理。

## Spring Boot 配置

- `spatial-twin-server/src/main/resources/application.yml`：公共配置、MyBatis-Plus、Sa-Token、MinIO 和平台安全配置，默认通过 `${config-active:dev}` 启用开发环境。
- `spatial-twin-server/src/main/resources/application-dev.yml`：本地 MySQL、Redis 和 MinIO 开发连接配置。
- `spatial-twin-server/src/main/resources/application-prod.yml`：生产连接配置和错误信息收敛配置。
- `../db/migration`：MySQL Flyway 迁移脚本，构建时由 `spatial-twin-server` 打包到 `classpath:db/migration`。

后端不依赖 `.env` 或 dotenv。默认直接加载 `application-dev.yml`；生产部署使用 `--config-active=prod` 或 `CONFIG_ACTIVE=prod` 切换，并通过部署环境提供 `JW_DB_PASSWORD` 等生产密钥。

每个子模块自己的职责与禁止事项见对应目录下的 `README.md`。
