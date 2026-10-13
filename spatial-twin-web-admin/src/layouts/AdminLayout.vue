<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Activity,
  Boxes,
  Building2,
  ClipboardList,
  Cuboid,
  FileClock,
  FolderKanban,
  Gauge,
  Layers3,
  LayoutDashboard,
  LogOut,
  Map,
  Menu,
  ShieldCheck,
  SlidersHorizontal,
  Users,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

interface NavigationItem {
  label: string
  path: string
  moduleKey: string
  icon: Component
}

interface NavigationGroup {
  label: string
  items: NavigationItem[]
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mobileMenuVisible = ref(false)
const loggingOut = ref(false)

const navigationGroups: NavigationGroup[] = [
  {
    label: '概览与空间',
    items: [
      { label: '工作台总览', path: '/', moduleKey: 'dashboard', icon: LayoutDashboard },
      { label: '园区项目', path: '/projects', moduleKey: 'projects', icon: FolderKanban },
      { label: '地图标注编辑器', path: '/map', moduleKey: 'map', icon: Map },
      { label: '楼宇精细编辑', path: '/building', moduleKey: 'building', icon: Building2 },
      { label: '楼层与区域管理', path: '/floor', moduleKey: 'floor', icon: Layers3 },
    ],
  },
  {
    label: '资产与交付',
    items: [
      { label: '资源资产中心', path: '/assets', moduleKey: 'assets', icon: Boxes },
      { label: '草稿与场景发布', path: '/release', moduleKey: 'release', icon: FileClock },
    ],
  },
  {
    label: '管理后台',
    items: [
      { label: '用户管理', path: '/system/users', moduleKey: 'users', icon: Users },
      { label: '角色管理', path: '/system/roles', moduleKey: 'roles', icon: ShieldCheck },
      { label: '菜单管理', path: '/system/menus', moduleKey: 'menus', icon: Menu },
      { label: '系统配置', path: '/system/settings', moduleKey: 'system-settings', icon: SlidersHorizontal },
      { label: '安全审计日志', path: '/audit', moduleKey: 'audit', icon: ShieldCheck },
      { label: '性能监控', path: '/performance', moduleKey: 'performance', icon: Gauge },
    ],
  },
]

const activeModule = computed(() => String(route.meta.moduleKey ?? ''))
const pageTitle = computed(() => String(route.meta.title ?? '管理平台'))
const visibleNavigationGroups = computed(() =>
  navigationGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        if (item.moduleKey === 'system-settings') return authStore.isSuperAdmin
        if (item.moduleKey === 'users') return authStore.hasPermission('system:user:list')
        if (item.moduleKey === 'roles') return authStore.hasPermission('system:role:list')
        if (item.moduleKey === 'menus') return authStore.hasPermission('system:menu:list')
        return true
      }),
    }))
    .filter((group) => group.items.length > 0),
)

function navigate(path: string) {
  mobileMenuVisible.value = false
  void router.push(path)
}

function showNotification() {
  ElMessage.info('通知中心将在后续里程碑接入。')
}

async function logout() {
  loggingOut.value = true
  try {
    await authStore.logout()
    ElMessage.success('已安全退出')
    await router.replace('/login')
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar desktop-sidebar">
      <div class="brand">
        <div class="brand-mark"><Cuboid :size="22" /></div>
        <div>
          <strong>经纬空间</strong>
          <span>JINGWEI SPATIAL TWIN</span>
        </div>
      </div>

      <nav class="navigation" aria-label="主导航">
        <section v-for="group in visibleNavigationGroups" :key="group.label" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.moduleKey"
            class="nav-item"
            :class="{ active: activeModule === item.moduleKey }"
            type="button"
            @click="navigate(item.path)"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </button>
        </section>
      </nav>

      <div class="sidebar-note">
        <Activity :size="16" />
        <span>账号：{{ authStore.user?.nickname || authStore.user?.username }}</span>
      </div>
    </aside>

    <el-drawer
      v-model="mobileMenuVisible"
      class="mobile-drawer"
      direction="ltr"
      size="280px"
      :show-close="false"
    >
      <div class="brand mobile-brand">
        <div class="brand-mark"><Cuboid :size="22" /></div>
        <div>
          <strong>经纬空间</strong>
          <span>管理平台</span>
        </div>
      </div>
      <nav class="navigation" aria-label="移动端主导航">
        <section v-for="group in visibleNavigationGroups" :key="group.label" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.moduleKey"
            class="nav-item"
            :class="{ active: activeModule === item.moduleKey }"
            type="button"
            @click="navigate(item.path)"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </button>
        </section>
      </nav>
    </el-drawer>

    <div class="page-shell">
      <header class="topbar">
        <div class="topbar-title">
          <el-button class="menu-button" text circle aria-label="打开导航" @click="mobileMenuVisible = true">
            <Menu :size="22" />
          </el-button>
          <div>
            <h1>{{ pageTitle }}</h1>
            <p>经纬空间 · 数字孪生管理平台</p>
          </div>
        </div>
        <div class="topbar-actions">
          <span class="environment-tag">本地演示环境</span>
          <el-tooltip content="查看通知" placement="bottom">
            <el-button circle aria-label="查看通知" @click="showNotification">
              <ClipboardList :size="17" />
            </el-button>
          </el-tooltip>
          <el-button :loading="loggingOut" @click="logout">
            <LogOut :size="16" />
            退出
          </el-button>
        </div>
      </header>

      <main class="page-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 20;
  display: flex;
  width: 252px;
  flex-direction: column;
  border-right: 1px solid #264752;
  background: #15333e;
  color: #e9f4f4;
}

.brand {
  display: flex;
  min-height: 76px;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgb(255 255 255 / 10%);
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid #66aea7;
  border-radius: 7px;
  background: #20535a;
  color: #9ee1d8;
}

.brand strong,
.brand span {
  display: block;
}

.brand strong {
  font-size: 17px;
  letter-spacing: 0;
}

.brand span {
  margin-top: 3px;
  color: #91aab1;
  font-size: 10px;
}

.navigation {
  flex: 1;
  overflow-y: auto;
  padding: 12px 10px 20px;
}

.nav-group + .nav-group {
  margin-top: 14px;
}

.nav-group-label {
  padding: 7px 12px;
  color: #7fa0a8;
  font-size: 11px;
  font-weight: 700;
}

.nav-item {
  display: flex;
  width: 100%;
  min-height: 42px;
  align-items: center;
  gap: 11px;
  margin: 2px 0;
  padding: 9px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #bfd0d4;
  text-align: left;
}

.nav-item:hover {
  background: rgb(255 255 255 / 6%);
  color: #fff;
}

.nav-item.active {
  background: #2b756f;
  color: #fff;
  box-shadow: inset 3px 0 #a4ded7;
}

.sidebar-note {
  display: flex;
  gap: 9px;
  margin: 10px;
  padding: 12px;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: 7px;
  color: #91aab1;
  font-size: 11px;
}

.sidebar-note svg {
  flex: 0 0 auto;
  margin-top: 2px;
}

.page-shell {
  min-height: 100vh;
  margin-left: 252px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  min-height: 76px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 28px;
  border-bottom: 1px solid var(--jw-border);
  background: rgb(255 255 255 / 94%);
  backdrop-filter: blur(12px);
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topbar h1 {
  margin: 0;
  font-size: 20px;
}

.topbar p {
  margin: 2px 0 0;
  color: var(--jw-muted);
  font-size: 12px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.environment-tag {
  padding: 5px 9px;
  border: 1px solid #b8d6d1;
  border-radius: 5px;
  background: #edf7f5;
  color: #246e68;
  font-size: 12px;
  white-space: nowrap;
}

.menu-button {
  display: none;
}

.page-content {
  padding: 24px 28px 36px;
}

.mobile-brand {
  min-height: auto;
  margin-bottom: 8px;
  padding: 4px 2px 14px;
  border-color: #e3eaec;
  color: var(--jw-text);
}

@media (max-width: 960px) {
  .desktop-sidebar {
    display: none;
  }

  .page-shell {
    margin-left: 0;
  }

  .menu-button {
    display: inline-flex;
  }
}

@media (max-width: 640px) {
  .topbar {
    padding: 10px 14px;
  }

  .topbar p,
  .environment-tag,
  .topbar-actions .el-button:last-child {
    display: none;
  }

  .page-content {
    padding: 16px 14px 28px;
  }
}
</style>
