# Jingwei Spatial Twin Frontend

经纬空间数字孪生管理平台前端，技术栈为 Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus。

## 本地开发

```powershell
pnpm.cmd install
pnpm.cmd dev
```

默认访问地址：`http://localhost:5173`

## 最小验证

```powershell
pnpm.cmd type-check
pnpm.cmd build
```

## 当前接口状态

- 登录、退出、当前用户已接入真实后端。
- 用户、角色、菜单和系统配置页面已接入真实后端，并包含加载、错误重试、权限按钮和删除确认。
- 项目、地图、楼宇、楼层、发布和资料等后续业务页面仍是明确标识的原型数据。
- 高德地图与 Three.js 正式场景尚未接入。

首次使用前需要由后端安全初始化管理员账号，具体方式见根目录 `db/README.md`。
