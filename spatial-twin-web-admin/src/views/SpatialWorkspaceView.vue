<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Box, Building2, Crosshair, Layers3, MapPin, Pentagon, Plus, Save } from '@lucide/vue'
import { ElMessage } from 'element-plus'

interface Building {
  id: string
  name: string
  businessId: string
  grade: string
  model: string
  x: string
  y: string
}

const router = useRouter()
const selectedId = ref('building-a')
const detailMode = ref(false)
const layerState = ref({
  boundary: true,
  buildings: true,
  devices: false,
  routes: false,
})

const buildings = ref<Building[]>([
  {
    id: 'building-a',
    name: 'A 栋 · 研发中心',
    businessId: 'Building-001',
    grade: '允许进入精细模式',
    model: '写实办公楼模板-v2',
    x: '27%',
    y: '34%',
  },
  {
    id: 'building-b',
    name: 'B 栋 · 行政办公',
    businessId: 'Building-002',
    grade: '默认标准写实',
    model: '写实办公楼模板-v1',
    x: '54%',
    y: '25%',
  },
  {
    id: 'building-c',
    name: 'C 栋 · 生产车间',
    businessId: 'Building-003',
    grade: '仅轮廓标注',
    model: '轻量厂房模板-v1',
    x: '67%',
    y: '58%',
  },
])

const selectedBuilding = computed(
  () => buildings.value.find((building) => building.id === selectedId.value) ?? buildings.value[0],
)

function updateSelectedName(value: string) {
  selectedBuilding.value.name = value
}

function updateSelectedGrade(value: string) {
  selectedBuilding.value.grade = value
}

function saveDraft() {
  ElMessage.success('前端草稿状态已更新，当前未连接后端持久化。')
}

function showMockAction(message: string) {
  ElMessage.info(`${message}将在后续空间引擎任务中接入。`)
}
</script>

<template>
  <div class="workspace-page">
    <section class="workspace-heading">
      <div>
        <span class="eyebrow">示范科技园区</span>
        <h2>园区地图标注工作台</h2>
        <p>空间对象、图层与建筑属性的前端交互骨架</p>
      </div>
      <div class="heading-actions">
        <span class="draft-state">草稿 · 本地模拟</span>
        <el-button @click="showMockAction('默认地图区域保存')">
          <Crosshair :size="16" />
          设为默认区域
        </el-button>
        <el-button type="primary" @click="saveDraft">
          <Save :size="16" />
          保存草稿
        </el-button>
      </div>
    </section>

    <section class="workspace-grid">
      <aside class="object-panel panel">
        <div class="panel-title">
          <h3>空间对象</h3>
          <el-tooltip content="新建楼宇" placement="top">
            <el-button circle size="small" aria-label="新建楼宇" @click="showMockAction('地图选点与轮廓绘制')">
              <Plus :size="15" />
            </el-button>
          </el-tooltip>
        </div>
        <el-input placeholder="搜索建筑或区域" clearable />
        <div class="tree-root"><Pentagon :size="16" /> 示范科技园区</div>
        <button
          v-for="building in buildings"
          :key="building.id"
          class="building-row"
          :class="{ active: selectedId === building.id }"
          type="button"
          @click="selectedId = building.id"
        >
          <Building2 :size="16" />
          <span>{{ building.name }}</span>
        </button>

        <div class="panel-divider" />
        <div class="panel-title"><h3>显示图层</h3></div>
        <el-checkbox v-model="layerState.boundary">园区边界</el-checkbox>
        <el-checkbox v-model="layerState.buildings">已标注楼宇</el-checkbox>
        <el-checkbox v-model="layerState.devices">设备点位</el-checkbox>
        <el-checkbox v-model="layerState.routes">专业线路</el-checkbox>
        <p class="panel-help">未标注建筑保留在底图层，不进入平台自有模型资产。</p>
      </aside>

      <div class="map-stage">
        <img src="/assets/demo-campus.png" alt="园区地图模拟背景" />
        <div v-if="layerState.boundary" class="campus-boundary" />
        <button
          v-for="building in layerState.buildings ? buildings : []"
          :key="building.id"
          class="map-marker"
          :class="{ active: selectedId === building.id }"
          :style="{ left: building.x, top: building.y }"
          type="button"
          @click="selectedId = building.id"
        >
          <MapPin :size="18" />
          <span>{{ building.name.split('·')[0] }}</span>
        </button>

        <div class="map-toolbar" aria-label="地图工具">
          <el-tooltip content="绘制园区边界" placement="top">
            <el-button circle aria-label="绘制园区边界" @click="showMockAction('园区边界绘制')">
              <Pentagon :size="17" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="新增楼宇" placement="top">
            <el-button circle aria-label="新增楼宇" @click="showMockAction('楼宇标注')">
              <Building2 :size="17" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="切换图层" placement="top">
            <el-button circle aria-label="切换图层" @click="showMockAction('地图图层切换')">
              <Layers3 :size="17" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="2D / 3D 模式" placement="top">
            <el-button circle aria-label="切换二维三维" @click="detailMode = !detailMode">
              <Box :size="17" />
            </el-button>
          </el-tooltip>
        </div>

        <div class="map-status">
          模拟底图 · 标准模型 3/3 · 精细模型 0/3 · {{ detailMode ? '3D 预览状态' : '2D 标注状态' }}
        </div>
      </div>

      <aside class="property-panel panel">
        <div class="panel-title">
          <h3>建筑属性</h3>
          <span class="object-id">{{ selectedBuilding.businessId }}</span>
        </div>
        <el-form label-position="top">
          <el-form-item label="建筑名称">
            <el-input :model-value="selectedBuilding.name" @update:model-value="updateSelectedName" />
          </el-form-item>
          <el-form-item label="对象业务 ID">
            <el-input :model-value="selectedBuilding.businessId" disabled />
          </el-form-item>
          <el-form-item label="展示精度">
            <el-select
              :model-value="selectedBuilding.grade"
              style="width: 100%"
              @update:model-value="updateSelectedGrade"
            >
              <el-option label="默认标准写实" value="默认标准写实" />
              <el-option label="允许进入精细模式" value="允许进入精细模式" />
              <el-option label="仅轮廓标注" value="仅轮廓标注" />
            </el-select>
          </el-form-item>
          <el-form-item label="当前模型">
            <el-input :model-value="selectedBuilding.model" disabled />
          </el-form-item>
        </el-form>
        <el-button class="wide-button" @click="router.push('/assets')">替换建筑模型</el-button>
        <el-button class="wide-button" type="primary" @click="router.push('/building')">
          进入楼宇精细视图
        </el-button>
        <div class="notice">精细模型只在用户主动进入后加载，本页面尚未接入 Three.js。</div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.workspace-page {
  display: grid;
  gap: 16px;
}

.workspace-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.workspace-heading h2 {
  margin: 3px 0;
  font-size: 23px;
}

.workspace-heading p {
  margin: 0;
  color: var(--jw-muted);
  font-size: 13px;
}

.eyebrow {
  color: var(--jw-accent);
  font-size: 11px;
  font-weight: 800;
}

.heading-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.draft-state {
  padding: 5px 8px;
  border: 1px solid #dfc3a4;
  border-radius: 5px;
  background: #fbf5ed;
  color: #94602e;
  font-size: 12px;
  white-space: nowrap;
}

.workspace-grid {
  display: grid;
  min-height: 650px;
  grid-template-columns: 230px minmax(420px, 1fr) 270px;
  overflow: hidden;
  border: 1px solid var(--jw-border);
  border-radius: var(--jw-radius);
  background: var(--jw-surface);
}

.panel {
  padding: 16px;
}

.object-panel {
  border-right: 1px solid var(--jw-border);
}

.property-panel {
  border-left: 1px solid var(--jw-border);
}

.panel-title {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.panel-title h3 {
  margin: 0;
  font-size: 15px;
}

.object-id {
  color: var(--jw-muted);
  font-family: Consolas, monospace;
  font-size: 10px;
}

.tree-root,
.building-row {
  display: flex;
  width: 100%;
  min-height: 40px;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--jw-text);
  font-size: 12px;
  text-align: left;
}

.tree-root {
  margin-top: 12px;
  color: #546c75;
  font-weight: 700;
}

.building-row {
  padding-left: 18px;
}

.building-row:hover {
  background: #f1f6f6;
}

.building-row.active {
  background: #e5f2ef;
  color: #17645f;
  font-weight: 700;
}

.panel-divider {
  height: 1px;
  margin: 14px 0;
  background: var(--jw-border);
}

.object-panel .el-checkbox {
  display: flex;
  margin: 5px 0;
}

.panel-help {
  margin: 14px 0 0;
  color: var(--jw-muted);
  font-size: 11px;
}

.map-stage {
  position: relative;
  min-height: 650px;
  overflow: hidden;
  background: #c8d6d5;
}

.map-stage > img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.78) contrast(0.95);
}

.campus-boundary {
  position: absolute;
  inset: 13% 9% 12% 8%;
  border: 2px dashed rgb(34 122 113 / 85%);
  background: rgb(73 158 144 / 8%);
  clip-path: polygon(9% 13%, 79% 4%, 95% 32%, 88% 90%, 17% 96%, 2% 60%);
}

.map-marker {
  position: absolute;
  z-index: 2;
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 5px;
  padding: 6px 9px;
  border: 1px solid #fff;
  border-radius: 5px;
  background: rgb(20 54 64 / 88%);
  color: #fff;
  box-shadow: 0 5px 16px rgb(10 40 48 / 20%);
  transform: translate(-50%, -50%);
  white-space: nowrap;
}

.map-marker.active {
  background: #b6672f;
  box-shadow: 0 0 0 4px rgb(255 255 255 / 46%);
}

.map-toolbar {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 3;
  display: flex;
  gap: 6px;
  padding: 6px;
  border: 1px solid rgb(255 255 255 / 75%);
  border-radius: 7px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 8px 24px rgb(20 52 60 / 14%);
}

.map-status {
  position: absolute;
  inset: auto 12px 12px;
  z-index: 3;
  padding: 8px 10px;
  border-radius: 5px;
  background: rgb(17 44 52 / 86%);
  color: #d8e9e9;
  font-size: 11px;
  text-align: center;
}

.wide-button {
  width: 100%;
  margin: 0 0 9px;
}

.wide-button + .wide-button {
  margin-left: 0;
}

.notice {
  margin-top: 10px;
  padding: 11px;
  border-left: 3px solid #c18246;
  background: #faf5ef;
  color: #72583f;
  font-size: 11px;
}

@media (max-width: 1180px) {
  .workspace-grid {
    grid-template-columns: 210px minmax(420px, 1fr);
  }

  .property-panel {
    grid-column: 1 / -1;
    border-top: 1px solid var(--jw-border);
    border-left: 0;
  }
}

@media (max-width: 760px) {
  .workspace-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .heading-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .object-panel {
    border-right: 0;
    border-bottom: 1px solid var(--jw-border);
  }

  .map-stage {
    min-height: 460px;
  }
}
</style>
