<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Archive,
  CalendarClock,
  FolderKanban,
  LayoutGrid,
  List as ListIcon,
  MapPin,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
} from '@lucide/vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  archiveProject,
  createProject,
  deleteProject,
  listProjects,
  updateProject,
} from '@/api/project'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/types/api'
import type {
  ProjectItem,
  ProjectMapMode,
  ProjectSaveRequest,
  ProjectStatus,
} from '@/types/project'

interface ProjectFormModel {
  projectCode: string
  name: string
  description: string
  locationName: string
  startup: {
    centerLng: number | null
    centerLat: number | null
    zoomLevel: number
    pitch: number
    rotation: number
    mapMode: ProjectMapMode
  }
}

// 项目页只展示当前用户有对象访问权的项目，按钮权限和项目角色以后端结果为准。
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const keyword = ref('')
const statusFilter = ref<ProjectStatus | ''>('')
const viewMode = ref<'card' | 'table'>('card')
const pageNo = ref(1)
const pageSize = 12
const total = ref(0)
const projects = ref<ProjectItem[]>([])
const formVisible = ref(false)
const editingProjectId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const projectForm = reactive<ProjectFormModel>({
  projectCode: '',
  name: '',
  description: '',
  locationName: '',
  startup: {
    centerLng: null,
    centerLat: null,
    zoomLevel: 16,
    pitch: 0,
    rotation: 0,
    mapMode: '2D',
  },
})

const formRules: FormRules = {
  projectCode: [
    { required: true, message: '请输入项目编码', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_-]{2,63}$/,
      message: '项目编码需以字母开头，仅包含字母、数字、短横线或下划线',
      trigger: 'blur',
    },
  ],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入项目位置名称', trigger: 'blur' }],
  'startup.centerLng': [{ required: true, message: '请输入默认中心经度', trigger: 'blur' }],
  'startup.centerLat': [{ required: true, message: '请输入默认中心纬度', trigger: 'blur' }],
  'startup.zoomLevel': [{ required: true, message: '请输入默认缩放级别', trigger: 'blur' }],
  'startup.pitch': [{ required: true, message: '请输入默认俯仰角', trigger: 'blur' }],
  'startup.rotation': [{ required: true, message: '请输入默认旋转角', trigger: 'blur' }],
  'startup.mapMode': [{ required: true, message: '请选择地图模式', trigger: 'change' }],
}

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

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

function projectMark(project: ProjectItem) {
  return project.name.trim().slice(0, 2) || '项目'
}

function memberRoleLabel(role: string | null) {
  const labels: Record<string, string> = {
    SUPER_ADMIN: '超级管理员',
    OWNER: '项目负责人',
    MANAGER: '项目管理员',
    VIEWER: '项目查看员',
  }
  return role ? (labels[role] ?? role) : '项目成员'
}

async function loadProjects() {
  loading.value = true
  loadError.value = ''
  try {
    const result = await listProjects({
      pageNo: pageNo.value,
      pageSize,
      keyword: keyword.value,
      status: statusFilter.value || undefined,
    })
    projects.value = result.records
    total.value = result.total
    if (pageNo.value > pageCount.value && total.value > 0) {
      pageNo.value = pageCount.value
      await loadProjects()
    }
  } catch (error) {
    loadError.value = errorMessage(error, '项目列表加载失败')
  } finally {
    loading.value = false
  }
}

async function searchProjects() {
  pageNo.value = 1
  await loadProjects()
}

function resetForm() {
  editingProjectId.value = null
  Object.assign(projectForm, {
    projectCode: '',
    name: '',
    description: '',
    locationName: '',
    startup: {
      centerLng: null,
      centerLat: null,
      zoomLevel: 16,
      pitch: 0,
      rotation: 0,
      mapMode: '2D',
    },
  })
}

function openCreateDialog() {
  resetForm()
  formVisible.value = true
}

function openEditDialog(project: ProjectItem) {
  editingProjectId.value = project.id
  Object.assign(projectForm, {
    projectCode: project.projectCode,
    name: project.name,
    description: project.description ?? '',
    locationName: project.locationName,
    startup: {
      centerLng: Number(project.startup.centerLng),
      centerLat: Number(project.startup.centerLat),
      zoomLevel: Number(project.startup.zoomLevel),
      pitch: Number(project.startup.pitch),
      rotation: Number(project.startup.rotation),
      mapMode: project.startup.mapMode,
    },
  })
  formVisible.value = true
}

function buildPayload(): ProjectSaveRequest | null {
  const { centerLng, centerLat, zoomLevel, pitch, rotation, mapMode } = projectForm.startup
  if (centerLng === null || centerLat === null) {
    return null
  }
  return {
    projectCode: projectForm.projectCode.trim(),
    name: projectForm.name.trim(),
    description: projectForm.description.trim(),
    locationName: projectForm.locationName.trim(),
    startup: {
      centerLng,
      centerLat,
      zoomLevel,
      pitch,
      rotation,
      mapMode,
    },
  }
}

async function submitProject() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = buildPayload()
  if (!payload) {
    ElMessage.warning('请填写完整的默认地图中心坐标')
    return
  }

  saving.value = true
  try {
    if (editingProjectId.value === null) {
      await createProject(payload)
      ElMessage.success('项目创建成功')
      pageNo.value = 1
    } else {
      await updateProject(editingProjectId.value, payload)
      ElMessage.success('项目修改成功')
    }
    formVisible.value = false
    await loadProjects()
  } catch (error) {
    ElMessage.error(errorMessage(error, '项目保存失败'))
  } finally {
    saving.value = false
  }
}

async function confirmArchive(project: ProjectItem) {
  try {
    await ElMessageBox.confirm(
      `确认归档项目“${project.name}”吗？归档后项目资料和地图参数将变为只读。`,
      '归档项目',
      {
        type: 'warning',
        confirmButtonText: '确认归档',
        cancelButtonText: '取消',
      },
    )
    await archiveProject(project.id)
    ElMessage.success('项目归档成功')
    await loadProjects()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '项目归档失败'))
  }
}

async function confirmDelete(project: ProjectItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除项目“${project.name}”吗？删除后项目将不再显示，相关历史审计仍会保留。`,
      '删除项目',
      {
        type: 'error',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteProject(project.id)
    ElMessage.success('项目删除成功')
    if (projects.value.length === 1 && pageNo.value > 1) {
      pageNo.value -= 1
    }
    await loadProjects()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '项目删除失败'))
  }
}

async function changePage(nextPage: number) {
  pageNo.value = nextPage
  await loadProjects()
}

onMounted(loadProjects)
</script>

<template>
  <section class="management-page project-page">
    <header class="management-heading">
      <div>
        <span class="management-eyebrow">项目与地图</span>
        <h2>园区项目</h2>
      </div>
      <el-button
        v-if="authStore.hasPermission('project:project:add')"
        type="primary"
        @click="openCreateDialog"
      >
        <Plus :size="17" />
        新建项目
      </el-button>
    </header>

    <div class="management-toolbar project-toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索项目编码、名称或位置"
        @keyup.enter="searchProjects"
        @clear="searchProjects"
      >
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-select
        v-model="statusFilter"
        clearable
        placeholder="全部状态"
        class="status-filter"
        @change="searchProjects"
        @clear="searchProjects"
      >
        <el-option label="进行中" value="ACTIVE" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <el-button :loading="loading" @click="loadProjects">
        <RefreshCw :size="16" />
        刷新
      </el-button>
      <div class="view-switch" aria-label="项目展示方式">
        <el-tooltip content="卡片视图" placement="top">
          <el-button
            :type="viewMode === 'card' ? 'primary' : ''"
            aria-label="切换为卡片视图"
            @click="viewMode = 'card'"
          >
            <LayoutGrid :size="16" />
          </el-button>
        </el-tooltip>
        <el-tooltip content="表格视图" placement="top">
          <el-button
            :type="viewMode === 'table' ? 'primary' : ''"
            aria-label="切换为表格视图"
            @click="viewMode = 'table'"
          >
            <ListIcon :size="17" />
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      class="management-alert"
    >
      <template #default>
        <el-button link type="primary" @click="loadProjects">重新加载</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="project-results">
      <el-empty
        v-if="!loading && !loadError && projects.length === 0"
        description="暂无符合条件的项目"
      >
        <el-button
          v-if="authStore.hasPermission('project:project:add')"
          type="primary"
          @click="openCreateDialog"
        >
          <Plus :size="16" />
          新建项目
        </el-button>
      </el-empty>

      <div v-else-if="viewMode === 'card'" class="project-grid">
        <article v-for="project in projects" :key="project.id" class="project-card">
          <div class="project-cover">
            <span class="project-mark">{{ projectMark(project) }}</span>
            <span class="project-code">{{ project.projectCode }}</span>
          </div>
          <div class="project-card-body">
            <div class="project-title-row">
              <div>
                <h3>{{ project.name }}</h3>
                <span>{{ memberRoleLabel(project.currentMemberRole) }}</span>
              </div>
              <el-tag
                :type="project.status === 'ACTIVE' ? 'success' : 'info'"
                effect="plain"
              >
                {{ project.status === 'ACTIVE' ? '进行中' : '已归档' }}
              </el-tag>
            </div>
            <p class="project-description">{{ project.description || '暂无项目说明' }}</p>
            <dl class="project-meta">
              <div>
                <dt><MapPin :size="15" />位置</dt>
                <dd>{{ project.locationName }}</dd>
              </div>
              <div>
                <dt><FolderKanban :size="15" />默认视角</dt>
                <dd>
                  {{ project.startup.mapMode }} · {{ formatCoordinate(project.startup.centerLng) }},
                  {{ formatCoordinate(project.startup.centerLat) }}
                </dd>
              </div>
              <div>
                <dt><CalendarClock :size="15" />更新时间</dt>
                <dd>{{ formatUtcTime(project.updatedAt) }}</dd>
              </div>
            </dl>
          </div>
          <footer class="project-actions">
            <el-button
              v-if="project.editable"
              link
              type="primary"
              @click="openEditDialog(project)"
            >
              <Pencil :size="15" />
              编辑
            </el-button>
            <el-button
              v-if="project.archivable"
              link
              type="danger"
              @click="confirmArchive(project)"
            >
              <Archive :size="15" />
              归档
            </el-button>
            <el-button
              v-if="project.deletable"
              link
              type="danger"
              @click="confirmDelete(project)"
            >
              <Trash2 :size="15" />
              删除
            </el-button>
          </footer>
        </article>
      </div>

      <div v-else class="management-table">
        <el-table :data="projects" empty-text="暂无符合条件的项目" style="width: 100%">
          <el-table-column label="项目" min-width="230">
            <template #default="{ row }">
              <div class="primary-cell">
                <span class="primary-cell-icon"><FolderKanban :size="17" /></span>
                <div>
                  <strong>{{ row.name }}</strong>
                  <small>{{ row.projectCode }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="locationName" label="位置" min-width="180" />
          <el-table-column label="默认地图" min-width="230">
            <template #default="{ row }">
              {{ row.startup.mapMode }} · {{ formatCoordinate(row.startup.centerLng) }},
              {{ formatCoordinate(row.startup.centerLat) }} · Z{{ row.startup.zoomLevel }}
            </template>
          </el-table-column>
          <el-table-column label="项目角色" min-width="125">
            <template #default="{ row }">{{ memberRoleLabel(row.currentMemberRole) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
                {{ row.status === 'ACTIVE' ? '进行中' : '已归档' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="180">
            <template #default="{ row }">{{ formatUtcTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="right">
            <template #default="{ row }">
              <el-button v-if="row.editable" link type="primary" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button
                v-if="row.archivable"
                link
                type="danger"
                @click="confirmArchive(row)"
              >
                归档
              </el-button>
              <el-button
                v-if="row.deletable"
                link
                type="danger"
                @click="confirmDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-if="total > 0" class="project-pagination">
      <span>共 {{ total }} 个项目</span>
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pageNo"
        :page-size="pageSize"
        :total="total"
        @current-change="changePage"
      />
    </div>

    <el-dialog
      v-model="formVisible"
      :title="editingProjectId === null ? '新建项目' : '编辑项目'"
      width="min(760px, calc(100vw - 32px))"
      destroy-on-close
      @closed="formRef?.clearValidate()"
    >
      <el-form
        ref="formRef"
        :model="projectForm"
        :rules="formRules"
        label-position="top"
        class="project-form"
      >
        <div class="form-grid">
          <el-form-item label="项目编码" prop="projectCode">
            <el-input
              v-model="projectForm.projectCode"
              maxlength="64"
              placeholder="例如 JW_PARK_01"
            />
          </el-form-item>
          <el-form-item label="项目名称" prop="name">
            <el-input v-model="projectForm.name" maxlength="128" placeholder="请输入项目名称" />
          </el-form-item>
        </div>
        <el-form-item label="项目位置" prop="locationName">
          <el-input
            v-model="projectForm.locationName"
            maxlength="255"
            placeholder="请输入园区、城市或详细位置名称"
          />
        </el-form-item>
        <el-form-item label="项目说明" prop="description">
          <el-input
            v-model="projectForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="可填写项目范围和用途"
          />
        </el-form-item>

        <div class="form-section-title">
          <MapPin :size="17" />
          默认地图视角
        </div>
        <div class="form-grid">
          <el-form-item label="中心经度" prop="startup.centerLng">
            <el-input-number
              v-model="projectForm.startup.centerLng"
              :min="-180"
              :max="180"
              :precision="7"
              :step="0.000001"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="中心纬度" prop="startup.centerLat">
            <el-input-number
              v-model="projectForm.startup.centerLat"
              :min="-90"
              :max="90"
              :precision="7"
              :step="0.000001"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="缩放级别" prop="startup.zoomLevel">
            <el-input-number
              v-model="projectForm.startup.zoomLevel"
              :min="3"
              :max="20"
              :precision="2"
              :step="0.5"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="地图模式" prop="startup.mapMode">
            <el-radio-group v-model="projectForm.startup.mapMode">
              <el-radio-button value="2D">二维</el-radio-button>
              <el-radio-button value="3D">三维</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="俯仰角" prop="startup.pitch">
            <el-input-number
              v-model="projectForm.startup.pitch"
              :min="0"
              :max="83"
              :precision="2"
              :step="1"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="旋转角" prop="startup.rotation">
            <el-input-number
              v-model="projectForm.startup.rotation"
              :min="0"
              :max="360"
              :precision="2"
              :step="1"
              controls-position="right"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitProject">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.project-page {
  min-width: 0;
  max-width: 100%;
}

.project-toolbar {
  flex-wrap: wrap;
}

.project-toolbar .status-filter {
  width: 150px;
}

.view-switch {
  display: flex;
  margin-left: auto;
}

.view-switch .el-button {
  width: 36px;
  padding: 0;
}

.view-switch .el-button + .el-button {
  margin-left: -1px;
}

.project-results {
  min-width: 0;
  max-width: 100%;
  min-height: 260px;
  overflow: hidden;
}

.project-results .management-table,
.project-results :deep(.el-table) {
  max-width: 100%;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.project-card {
  display: grid;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--jw-border);
  border-radius: var(--jw-radius);
  background: var(--jw-surface);
  grid-template-rows: 92px 1fr auto;
}

.project-cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding: 18px;
  background: #e8f1f2;
  color: #24515d;
}

.project-mark {
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  border: 1px solid #9fc2c5;
  border-radius: 6px;
  background: #fff;
  color: var(--jw-accent-dark);
  font-size: 16px;
  font-weight: 800;
}

.project-code {
  z-index: 1;
  max-width: 190px;
  overflow: hidden;
  color: #476871;
  font-family: Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-card-body {
  min-width: 0;
  padding: 16px 18px;
}

.project-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.project-title-row h3 {
  margin: 0;
  font-size: 17px;
}

.project-title-row span {
  color: var(--jw-muted);
  font-size: 12px;
}

.project-description {
  display: -webkit-box;
  min-height: 42px;
  margin: 12px 0 14px;
  overflow: hidden;
  color: #566d76;
  font-size: 13px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.project-meta {
  display: grid;
  gap: 9px;
  margin: 0;
}

.project-meta div {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 8px;
}

.project-meta dt {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--jw-muted);
  font-size: 12px;
}

.project-meta dd {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #2d4954;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-actions {
  display: flex;
  justify-content: flex-end;
  min-height: 48px;
  padding: 8px 14px;
  border-top: 1px solid #edf1f2;
}

.project-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--jw-muted);
  font-size: 12px;
}

.form-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 14px;
  padding-top: 16px;
  border-top: 1px solid var(--jw-border);
  color: #294c58;
  font-size: 14px;
  font-weight: 700;
}

.project-form :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 760px) {
  .project-toolbar .status-filter,
  .view-switch {
    width: 100%;
  }

  .view-switch {
    margin-left: 0;
  }

  .view-switch .el-button {
    width: 50%;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }

  .project-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
