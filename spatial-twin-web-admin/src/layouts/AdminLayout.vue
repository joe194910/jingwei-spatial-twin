<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Activity,
  Boxes,
  Building2,
  ClipboardList,
  Circle,
  Cuboid,
  FileClock,
  FolderKanban,
  Gauge,
  Layers3,
  LayoutDashboard,
  LogOut,
  Map,
  Menu,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Users,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'
import type { MenuItem } from '@/types/system'

interface NavigationItem {
  id: number
  label: string
  path: string
  icon: Component
}

interface NavigationGroup {
  id: number
  label: string
  items: NavigationItem[]
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mobileMenuVisible = ref(false)
const loggingOut = ref(false)

const pageTitle = computed(() => String(route.meta.title ?? '管理平台'))
const iconRegistry: Record<string, Component> = {
  Activity,
  Boxes,
  Building2,
  Circle,
  FileClock,
  FolderKanban,
  Gauge,
  Layers3,
  LayoutDashboard,
  Map,
  Menu,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Users,
}

function resolveIcon(iconName: string | null) {
  if (!iconName) return Circle
  return iconRegistry[iconName] ?? Circle
}

function flattenNavigationItems(menus: MenuItem[]): NavigationItem[] {
  return menus.flatMap((menu) => {
    const currentItem = menu.path.startsWith('/') && menu.component !== 'Layout'
      ? [{
          id: menu.id,
          label: menu.name,
          path: menu.path,
          icon: resolveIcon(menu.icon),
        }]
      : []
    return [...currentItem, ...flattenNavigationItems(menu.children)]
  })
}

// 根菜单名称作为侧栏分组标题，全部导航内容以后台菜单树为唯一数据源。
const visibleNavigationGroups = computed<NavigationGroup[]>(() =>
  authStore.menus
    .map((group) => ({
      id: group.id,
      label: group.name,
      items: flattenNavigationItems(group.children.length > 0 ? group.children : [group]),
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
        <section v-for="group in visibleNavigationGroups" :key="group.id" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.id"
            class="nav-item"
            :class="{ active: route.path === item.path }"
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
        <section v-for="group in visibleNavigationGroups" :key="group.id" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.id"
            class="nav-item"
            :class="{ active: route.path === item.path }"
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
