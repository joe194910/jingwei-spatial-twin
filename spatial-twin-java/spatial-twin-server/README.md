# spatial-twin-server

Spring Boot 可执行装配模块，是后端唯一启动入口。

## 当前职责

- `SpatialApplication` 启动类
- Spring、Sa-Token、MyBatis-Plus 等框架配置
- 管理员初始化等应用启动任务
- YAML 环境配置
- 健康检查接口
- 汇总业务模块并打包根目录 `db/migration`

## 边界

- 只做启动、配置和模块装配，不实现用户、项目、楼宇等业务逻辑。
- 不定义业务 Entity、DTO、Mapper 或 Service。
- 新业务进入对应的 `spatial-twin-module-*`，由本模块增加 Maven 依赖完成装配。

启动类：`src/main/java/com/jingwei/spatial/SpatialApplication.java`

配置目录：`src/main/resources`

## 本地开发配置

开发环境默认直接加载 `src/main/resources/application-dev.yml` 中现有的本机基础设施配置。
需要临时覆盖连接参数时可使用对应环境变量；使用 `--config-active=prod` 或
`CONFIG_ACTIVE=prod` 可切换到生产配置。
