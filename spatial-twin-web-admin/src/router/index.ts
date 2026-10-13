import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { setUnauthorizedHandler } from '@/api/http'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import DashboardView from '@/views/DashboardView.vue'
import LoginView from '@/views/LoginView.vue'
import ModuleView from '@/views/ModuleView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import SpatialWorkspaceView from '@/views/SpatialWorkspaceView.vue'
import MenuManagementView from '@/views/system/MenuManagementView.vue'
import RoleManagementView from '@/views/system/RoleManagementView.vue'
import SystemSettingView from '@/views/system/SystemSettingView.vue'
import UserManagementView from '@/views/system/UserManagementView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: '工作台总览', moduleKey: 'dashboard' },
      },
      {
        path: 'projects',
        name: 'projects',
        component: ModuleView,
        meta: { title: '园区项目', moduleKey: 'projects' },
      },
      {
        path: 'map',
        name: 'map',
        component: SpatialWorkspaceView,
        meta: { title: '地图标注编辑器', moduleKey: 'map' },
      },
      {
        path: 'building',
        name: 'building',
        component: ModuleView,
        meta: { title: '楼宇精细编辑', moduleKey: 'building' },
      },
      {
        path: 'floor',
        name: 'floor',
        component: ModuleView,
        meta: { title: '楼层与区域管理', moduleKey: 'floor' },
      },
      {
        path: 'assets',
        name: 'assets',
        component: ModuleView,
        meta: { title: '资源资产中心', moduleKey: 'assets' },
      },
      {
        path: 'release',
        name: 'release',
        component: ModuleView,
        meta: { title: '草稿与场景发布', moduleKey: 'release' },
      },
      {
        path: 'system/users',
        name: 'users',
        component: UserManagementView,
        meta: { title: '用户管理', moduleKey: 'users', permission: 'system:user:list' },
      },
      {
        path: 'system/roles',
        name: 'roles',
        component: RoleManagementView,
        meta: { title: '角色管理', moduleKey: 'roles', permission: 'system:role:list' },
      },
      {
        path: 'system/menus',
        name: 'menus',
        component: MenuManagementView,
        meta: { title: '菜单管理', moduleKey: 'menus', permission: 'system:menu:list' },
      },
      {
        path: 'system/settings',
        name: 'system-settings',
        component: SystemSettingView,
        meta: { title: '系统配置', moduleKey: 'system-settings', role: 'SUPER_ADMIN' },
      },
      {
        path: 'audit',
        name: 'audit',
        component: ModuleView,
        meta: { title: '安全审计日志', moduleKey: 'audit' },
      },
      {
        path: 'settings',
        redirect: '/system/settings',
      },
      {
        path: 'performance',
        name: 'performance',
        component: ModuleView,
        meta: { title: '性能监控', moduleKey: 'performance' },
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '登录与注册' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  await authStore.restoreSession()

  if (to.name === 'login') {
    if (!authStore.isLoggedIn) {
      return true
    }
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/'
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  if (requiresAuth && !authStore.isLoggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  const requiredRole = to.meta.role
  if (typeof requiredRole === 'string' && !authStore.user?.roles.includes(requiredRole)) {
    return '/'
  }

  const requiredPermission = to.meta.permission
  if (typeof requiredPermission === 'string' && !authStore.hasPermission(requiredPermission)) {
    return '/'
  }

  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '管理平台')} - 经纬空间`
})

setUnauthorizedHandler(() => {
  const authStore = useAuthStore(pinia)
  authStore.$reset()
  const currentPath = router.currentRoute.value.fullPath
  if (router.currentRoute.value.name !== 'login') {
    void router.replace({
      name: 'login',
      query: { redirect: currentPath },
    })
  }
})

export default router
