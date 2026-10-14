<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Archive,
  Building2,
  ChevronRight,
  Crosshair,
  FolderKanban,
  LocateFixed,
  Map,
  MapPin,
  PanelLeftOpen,
  Plus,
  RefreshCw,
  Search,
  X,
} from '@lucide/vue'

import { listProjects } from '@/api/project'
import JingweiOverviewMap from '@/components/map/JingweiOverviewMap.vue'
import type { MapProviderState, OverviewProjectMarker } from '@/map/types'
import { ApiError } from '@/types/api'
import type { ProjectItem } from '@/types/project'

interface OverviewMapExpose {
  fitProjects: () => void
  focusProject: (projectId: string) => void
  retry: () => Promise<void>
}

// 首页只消费用户有权访问的项目轻量信息，具体地图渲染由适配器负责。
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const keyword = ref('')
const projects = ref<ProjectItem[]>([])
const selectedProjectId = ref<string | null>(null)
const projectBrowserOpen = ref(false)
const overviewMap = ref<OverviewMapExpose>()
const mapProviderState = ref<MapProviderState>({
  mode: 'OFFLINE',
  label: '经纬离线底座',
  degraded: true,
  reason: '正在初始化地图底座',
})

const filteredProjects = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLocaleLowerCase('zh-CN')
  if (!normalizedKeyword) {
    return projects.value
  }
  return projects.value.filter((project) =>
    [project.name, project.projectCode, project.locationName].some((value) =>
      value.toLocaleLowerCase('zh-CN').includes(normalizedKeyword),
    ),
  )
})

const projectMarkers = computed<OverviewProjectMarker[]>(() =>
  filteredProjects.value.map((project) => ({
    id: project.id,
    name: project.name,
    locationName: project.locationName,
    status: project.status,
    coordinate: {
      lng: Number(project.startup.centerLng),
      lat: Number(project.startup.centerLat),
    },
    viewpoint: {
      mapMode: project.startup.mapMode,
      zoom: Number(project.startup.zoomLevel),
      pitch: Number(project.startup.pitch),
      rotation: Number(project.startup.rotation),
    },
  })),
)

const selectedProject = computed(
  () => projects.value.find((project) => project.id === selectedProjectId.value) ?? null,
)

const activeProjectCount = computed(
  () => projects.value.filter((project) => project.status === 'ACTIVE').length,
)

const archivedProjectCount = computed(
  () => projects.value.filter((project) => project.status === 'ARCHIVED').length,
)

watch(filteredProjects, (visibleProjects) => {
  const selectedProjectVisible = visibleProjects.some(
    (project) => project.id === selectedProjectId.value,
  )
  if (!selectedProjectVisible) {
    selectedProjectId.value = null
  }
})

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function formatUtcTime(value: string) {
  const normalized = /Z$|[+-]\d{2}:\d{2}$/.test(value) ? value : `${value}Z`
  const date = new Date(normalized)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

function formatCoordinate(value: number) {
  return Number(value).toFixed(6)
}

async function loadProjects() {
  loading.value = true
  loadError.value = ''
  try {
    const result = await listProjects({
      pageNo: 1,
      pageSize: 100,
    })
    projects.value = result.records
    const selectedStillExists = projects.value.some(
      (project) => project.id === selectedProjectId.value,
    )
    if (!selectedStillExists) {
      selectedProjectId.value = null
    }
  } catch (error) {
    loadError.value = errorMessage(error, '空间总览加载失败')
  } finally {
    loading.value = false
  }
}

function selectProject(projectId: string) {
  selectedProjectId.value = projectId
  projectBrowserOpen.value = false
  overviewMap.value?.focusProject(projectId)
}

function clearSelectedProject() {
  selectedProjectId.value = null
}

function resetViewport() {
  clearSelectedProject()
  overviewMap.value?.fitProjects()
}

function updateMapProviderState(state: MapProviderState) {
  mapProviderState.value = state
}

async function refreshOverview() {
  await Promise.all([loadProjects(), overviewMap.value?.retry()])
}

function openProject(project: ProjectItem) {
  void router.push({
    path: '/map',
    query: { projectId: project.id },
  })
}

function openProjectManagement() {
  void router.push('/projects')
}

onMounted(loadProjects)
</script>

<template>
  <section class="spatial-overview" aria-label="经纬空间总览">
    <JingweiOverviewMap
      ref="overviewMap"
      :projects="projectMarkers"
      :selected-project-id="selectedProjectId"
      @select="selectProject"
      @provider-state="updateMapProviderState"
    />

    <header class="overview-command">
      <div class="overview-title">
        <span class="overview-symbol"><Map :size="18" /></span>
        <div>
          <strong>经纬空间总览</strong>
          <small>所有园区与楼宇的统一空间入口</small>
        </div>
      </div>
      <div class="overview-command-actions">
        <el-tooltip
          :content="mapProviderState.reason || '在线地图运行正常'"
          placement="bottom"
        >
          <span class="provider-state">
            <span class="provider-dot" :class="{ online: !mapProviderState.degraded }" />
            {{ mapProviderState.label }}
          </span>
        </el-tooltip>
        <el-tooltip content="重置项目视野" placement="bottom">
          <el-button circle aria-label="重置项目视野" @click="resetViewport">
            <LocateFixed :size="17" />
          </el-button>
        </el-tooltip>
        <el-tooltip content="刷新空间总览" placement="bottom">
          <el-button circle :loading="loading" aria-label="刷新空间总览" @click="refreshOverview">
            <RefreshCw v-if="!loading" :size="17" />
          </el-button>
        </el-tooltip>
      </div>
    </header>

    <el-tooltip v-if="!projectBrowserOpen" content="打开项目列表" placement="right">
      <el-badge :value="projects.length" class="project-browser-trigger">
        <el-button circle aria-label="打开项目列表" @click="projectBrowserOpen = true">
          <PanelLeftOpen :size="18" />
        </el-button>
      </el-badge>
    </el-tooltip>

    <aside v-if="projectBrowserOpen" class="project-browser">
      <div class="browser-heading">
        <div>
          <span>空间项目</span>
          <strong>{{ projects.length }}</strong>
        </div>
        <div class="browser-actions">
          <el-tooltip content="管理园区项目" placement="top">
            <el-button circle size="small" aria-label="管理园区项目" @click="openProjectManagement">
              <Plus :size="15" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="收起项目列表" placement="top">
            <el-button circle size="small" aria-label="收起项目列表" @click="projectBrowserOpen = false">
              <X :size="15" />
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <el-input v-model="keyword" clearable placeholder="搜索项目或位置">
        <template #prefix><Search :size="15" /></template>
      </el-input>

      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        :closable="false"
        show-icon
        class="overview-alert"
      >
        <template #default>
          <el-button link type="primary" @click="loadProjects">重新加载</el-button>
        </template>
      </el-alert>

      <div v-loading="loading" class="project-list">
        <button
          v-for="project in filteredProjects"
          :key="project.id"
          class="project-row"
          :class="{ active: project.id === selectedProjectId }"
          type="button"
          @click="selectProject(project.id)"
        >
          <span class="project-row-icon">
            <FolderKanban :size="16" />
          </span>
          <span class="project-row-copy">
            <strong>{{ project.name }}</strong>
            <small>{{ project.locationName }}</small>
          </span>
          <span class="project-row-state" :class="{ archived: project.status === 'ARCHIVED' }">
            {{ project.status === 'ACTIVE' ? '进行中' : '已归档' }}
          </span>
        </button>

        <div v-if="!loading && !loadError && filteredProjects.length === 0" class="empty-projects">
          <MapPin :size="26" />
          <strong>{{ projects.length === 0 ? '还没有园区项目' : '没有匹配的项目' }}</strong>
          <small>
            {{ projects.length === 0 ? '创建项目后会显示在空间底座上' : '请尝试其他搜索关键词' }}
          </small>
          <el-button v-if="projects.length === 0" type="primary" @click="openProjectManagement">
            创建园区项目
          </el-button>
        </div>
      </div>
    </aside>

    <aside v-if="selectedProject" class="project-inspector">
      <div class="inspector-heading">
        <div class="inspector-identity">
          <span class="inspector-icon"><Building2 :size="18" /></span>
          <div>
            <small>{{ selectedProject.projectCode }}</small>
            <h2>{{ selectedProject.name }}</h2>
          </div>
        </div>
        <el-tooltip content="关闭项目详情" placement="left">
          <el-button circle size="small" aria-label="关闭项目详情" @click="clearSelectedProject">
            <X :size="15" />
          </el-button>
        </el-tooltip>
      </div>
      <p>{{ selectedProject.description || '该项目暂未填写说明。' }}</p>
      <dl>
        <div>
          <dt>项目位置</dt>
          <dd>{{ selectedProject.locationName }}</dd>
        </div>
        <div>
          <dt>默认中心</dt>
          <dd>
            {{ formatCoordinate(selectedProject.startup.centerLng) }},
            {{ formatCoordinate(selectedProject.startup.centerLat) }}
          </dd>
        </div>
        <div>
          <dt>地图视角</dt>
          <dd>
            {{ selectedProject.startup.mapMode }} · Z{{ selectedProject.startup.zoomLevel }}
          </dd>
        </div>
        <div>
          <dt>最近更新</dt>
          <dd>{{ formatUtcTime(selectedProject.updatedAt) }}</dd>
        </div>
      </dl>
      <el-button
        type="primary"
        :disabled="selectedProject.status === 'ARCHIVED'"
        @click="openProject(selectedProject)"
      >
        打开标注工作台
        <ChevronRight :size="16" />
      </el-button>
    </aside>

    <footer class="overview-status">
      <span><Crosshair :size="14" />项目坐标来自已保存的默认地图中心</span>
      <span><FolderKanban :size="14" />{{ activeProjectCount }} 个进行中</span>
      <span><Archive :size="14" />{{ archivedProjectCount }} 个已归档</span>
      <span class="status-note">
        {{
          mapProviderState.degraded
            ? mapProviderState.reason || '在线地图不可用，当前使用经纬离线底座'
            : '在线底图由高德提供，空间业务数据由经纬空间管理'
        }}
      </span>
    </footer>
  </section>
</template>

<style scoped>
.spatial-overview {
  position: relative;
  min-width: 0;
  min-height: calc(100vh - 76px);
  margin: -24px -28px -36px;
  overflow: hidden;
  background: #dfe8e5;
}

.overview-command,
.project-browser-trigger,
.project-browser,
.project-inspector,
.overview-status {
  position: absolute;
  z-index: 4;
  border: 1px solid rgb(113 143 138 / 35%);
  background: rgb(250 252 251 / 94%);
  box-shadow: 0 12px 30px rgb(32 67 64 / 13%);
  backdrop-filter: blur(12px);
}

.overview-command {
  top: 18px;
  left: 18px;
  display: flex;
  width: min(650px, calc(100% - 220px));
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 10px 8px 14px;
  border-radius: 7px;
}

.overview-title,
.overview-command-actions,
.browser-heading,
.browser-heading > div,
.inspector-heading,
.overview-status,
.overview-status span {
  display: flex;
  align-items: center;
}

.overview-title {
  min-width: 0;
  gap: 10px;
}

.overview-title > div {
  display: grid;
}

.overview-title strong {
  font-size: 15px;
}

.overview-title small {
  color: var(--jw-muted);
  font-size: 11px;
}

.overview-symbol,
.inspector-icon,
.project-row-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 5px;
  background: #e2f0ed;
  color: #17645f;
}

.overview-symbol {
  width: 34px;
  height: 34px;
}

.overview-command-actions {
  gap: 7px;
}

.provider-state {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-right: 3px;
  color: #506c72;
  font-size: 11px;
  white-space: nowrap;
}

.provider-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b87335;
  box-shadow: 0 0 0 3px rgb(184 115 53 / 14%);
}

.provider-dot.online {
  background: #2f8d68;
  box-shadow: 0 0 0 3px rgb(47 141 104 / 14%);
}

.project-browser {
  top: 92px;
  bottom: 50px;
  left: 18px;
  display: grid;
  width: 286px;
  min-height: 0;
  padding: 14px;
  border-radius: 7px;
  grid-template-rows: auto auto minmax(0, 1fr);
}

.project-browser-trigger {
  top: 92px;
  left: 18px;
  border: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.project-browser-trigger :deep(.el-badge__content) {
  border: 2px solid #f8fbfa;
  background: #267d76;
}

.browser-heading {
  min-height: 34px;
  justify-content: space-between;
  margin-bottom: 10px;
}

.browser-actions,
.inspector-identity {
  display: flex;
  align-items: center;
}

.browser-actions {
  gap: 6px;
}

.browser-heading > div {
  gap: 8px;
}

.browser-heading span {
  color: #466069;
  font-size: 13px;
  font-weight: 700;
}

.browser-heading strong {
  color: var(--jw-accent);
  font-size: 18px;
}

.overview-alert {
  margin-top: 10px;
}

.project-list {
  min-height: 0;
  margin: 12px -5px 0;
  overflow-y: auto;
  padding: 0 5px 8px;
  scrollbar-width: thin;
  scrollbar-color: #aac3bf transparent;
}

.project-row {
  display: grid;
  width: 100%;
  min-height: 64px;
  align-items: center;
  gap: 9px;
  padding: 9px;
  border: 0;
  border-bottom: 1px solid #e5eceb;
  background: transparent;
  color: var(--jw-text);
  grid-template-columns: 32px minmax(0, 1fr) auto;
  text-align: left;
}

.project-row:hover {
  background: #f0f6f4;
}

.project-row.active {
  background: #e5f1ee;
  box-shadow: inset 3px 0 #267d76;
}

.project-row-icon {
  width: 32px;
  height: 32px;
}

.project-row-copy {
  display: grid;
  min-width: 0;
}

.project-row-copy strong,
.project-row-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-row-copy strong {
  font-size: 12px;
}

.project-row-copy small {
  color: var(--jw-muted);
  font-size: 10px;
}

.project-row-state {
  color: #267d76;
  font-size: 10px;
  white-space: nowrap;
}

.project-row-state.archived {
  color: #7d898d;
}

.empty-projects {
  display: grid;
  min-height: 250px;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 20px;
  color: var(--jw-muted);
  text-align: center;
}

.empty-projects strong {
  color: #415b64;
  font-size: 13px;
}

.empty-projects small {
  font-size: 11px;
}

.empty-projects .el-button {
  margin-top: 6px;
}

.project-inspector {
  right: 18px;
  bottom: 50px;
  width: 320px;
  padding: 16px;
  border-radius: 7px;
}

.inspector-heading {
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.inspector-identity {
  min-width: 0;
  align-items: flex-start;
  gap: 10px;
}

.inspector-icon {
  width: 36px;
  height: 36px;
}

.inspector-identity > div {
  min-width: 0;
}

.inspector-heading small {
  color: var(--jw-accent);
  font-family: Consolas, monospace;
  font-size: 10px;
}

.inspector-heading h2 {
  margin: 1px 0 0;
  font-size: 17px;
}

.project-inspector > p {
  display: -webkit-box;
  margin: 13px 0;
  overflow: hidden;
  color: #5f747b;
  font-size: 11px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.project-inspector dl {
  display: grid;
  gap: 8px;
  margin: 0 0 14px;
}

.project-inspector dl div {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 8px;
  font-size: 11px;
}

.project-inspector dt {
  color: var(--jw-muted);
}

.project-inspector dd {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #2c4b55;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-inspector .el-button {
  width: 100%;
}

.overview-status {
  right: auto;
  bottom: 12px;
  left: 50%;
  width: max-content;
  max-width: calc(100% - 360px);
  min-height: 30px;
  gap: 18px;
  padding: 5px 10px;
  border-radius: 5px;
  color: #506b71;
  font-size: 10px;
  transform: translateX(-50%);
}

.overview-status span {
  gap: 5px;
  white-space: nowrap;
}

.overview-status .status-note {
  color: #8a623d;
}

@media (max-width: 1100px) {
  .overview-command {
    width: min(570px, calc(100% - 190px));
  }

  .overview-status {
    max-width: calc(100% - 160px);
  }

  .project-inspector {
    width: 290px;
  }
}

@media (max-width: 820px) {
  .spatial-overview {
    display: grid;
    min-height: auto;
    margin: -16px -14px -28px;
    overflow: visible;
    background: #dfe8e5;
    grid-template-rows: 520px auto auto auto;
  }

  .overview-command {
    right: 12px;
    left: 12px;
    width: auto;
  }

  .project-browser-trigger {
    top: 92px;
    left: 12px;
  }

  .project-browser,
  .project-inspector,
  .overview-status {
    position: static;
    width: auto;
    margin: 0;
    border-right: 0;
    border-bottom: 0;
    border-left: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .project-browser {
    min-height: 360px;
    grid-row: 2;
  }

  .project-list {
    max-height: 330px;
  }

  .project-inspector {
    grid-row: 3;
  }

  .overview-status {
    grid-row: 4;
    flex-wrap: wrap;
    gap: 7px 14px;
    padding: 10px 14px;
    max-width: none;
    transform: none;
  }

  .overview-status .status-note {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 560px) {
  .overview-command {
    align-items: flex-start;
  }

  .overview-title small,
  .provider-state {
    display: none;
  }
}
</style>
