# 数据库脚本

该目录统一管理经纬空间数字孪生管理平台的数据库脚本。

## 目录约定

- `migration/`：由 Flyway 按版本顺序自动执行的 MySQL 8.0 数据库变更脚本。
- 脚本命名使用 `V{版本号}__{英文说明}.sql`，已执行的版本脚本不得直接修改。
- 表、字段、索引、约束、基础权限和必要初始化数据的变更应通过迁移脚本交付，不在 Java 代码中临时建表。
- 本目录禁止存放业务查询 SQL。分页、筛选、联表和报表查询应放在对应后端业务模块的 Mapper Java/XML 中。

后端 `spatial-twin-java/spatial-twin-server` 模块构建时会将本目录打包到 `classpath:db`，当前 Flyway 扫描位置为 `classpath:db/migration`。

## 本地首次初始化

迁移脚本由 Spring Boot 启动时的 Flyway 自动执行，不要在数据库客户端中重复执行 `V1__initialize_rbac.sql`。

首次使用只需要创建空数据库：

```sql
CREATE DATABASE IF NOT EXISTS jingwei_spatial
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

然后启动后端：

```powershell
cd spatial-twin-java
mvn -pl spatial-twin-server -am spring-boot:run
```

Flyway 会创建 `flyway_schema_history`，执行尚未执行的版本脚本，并记录版本、校验和与执行结果。已经成功执行的迁移不得再次手工执行，也不得直接修改原脚本。

## 首个管理员初始化

源码和迁移脚本不会提供默认管理员密码。首次启动时由操作者临时设置环境变量，密码必须为自行指定的 12 至 128 位安全密码：

```powershell
cd spatial-twin-java
$env:JW_BOOTSTRAP_ADMIN_ENABLED='true'
$env:JW_BOOTSTRAP_ADMIN_USERNAME='admin'
$env:JW_BOOTSTRAP_ADMIN_PASSWORD='在此输入自行指定的安全密码'
mvn -pl spatial-twin-server -am spring-boot:run
```

初始化器只会在账号不存在时创建管理员，不会覆盖已有账号密码。确认管理员创建成功后，应关闭初始化开关并重新启动：

```powershell
$env:JW_BOOTSTRAP_ADMIN_ENABLED='false'
```

管理员密码不得写入 YAML、SQL、Java、TypeScript、README 或版本控制记录。

## `Table already exists` 排查

出现 `ERROR 1050 (42S01): Table 'jw_sys_user' already exists`，说明目标库中已经存在表，但当前操作仍在尝试执行 V1。先检查迁移记录：

```sql
USE jingwei_spatial;

SHOW TABLES LIKE 'flyway_schema_history';

SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

- 如果存在 `version = '1'` 且 `success = 1` 的记录：V1 已执行完成，停止手工执行 SQL，直接启动后端。
- 如果没有 `flyway_schema_history`，且当前只是可清理的本地开发库：删除并重建空数据库，再由 Flyway 初始化。
- 如果没有迁移记录但库中已有需要保留的数据：先备份并核对全部表结构，不得直接删表、添加 `IF NOT EXISTS` 或强行写入迁移记录；确认现有结构完整后再制定基线迁移方案。

本地开发库确认不需要保留数据时，可执行：

```sql
DROP DATABASE jingwei_spatial;

CREATE DATABASE jingwei_spatial
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

重建后不要再手工运行 V1，启动后端即可。
