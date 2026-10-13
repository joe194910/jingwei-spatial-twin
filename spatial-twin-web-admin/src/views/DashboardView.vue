<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowRight, Building2, CircleAlert, Cuboid, FolderKanban, MapPinned } from '@lucide/vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const metrics = [
  { label: '已创建项目', value: '02', detail: '个人工作区', icon: FolderKanban, tone: 'teal' },
  { label: '已标注楼宇', value: '13', detail: '其中精细楼宇 3', icon: Building2, tone: 'blue' },
  { label: '模型资产', value: '31', detail: '标准与精细分离', icon: Cuboid, tone: 'amber' },
  { label: '待处理事项', value: '02', detail: '需要校准', icon: CircleAlert, tone: 'red' },
]

const tasks = [
  { name: '办公楼 A 栋高清模型', state: '已处理', type: 'success' as const },
  { name: '6 楼消防图纸校准', state: '待确认', type: 'warning' as const },
  { name: '项目权限策略', state: '待接入', type: 'info' as const },
]

const projects = [
  { name: '示范科技园区', buildings: 5, detail: 1, status: '草稿', updatedAt: '本地演示' },
  { name: '智慧住宅样板', buildings: 8, detail: 2, status: '已发布', updatedAt: '本地演示' },
]

function openMap() {
  void router.push('/map')
}

function showPendingMessage() {
  ElMessage.info('后端任务中心将在 M1 之后接入。')
}
</script>

<template>
  <div class="dashboard">
    <section class="page-heading">
      <div>
        <span class="eyebrow">空间运营概览</span>
        <h2>欢迎回来，空间管理员</h2>
        <p>从园区标注开始，逐步建立可编辑、可发布的数字空间。</p>
      </div>
      <el-button type="primary" size="large" @click="openMap">
        <MapPinned :size="18" />
        进入园区地图
      </el-button>
    </section>

    <section class="metric-grid" aria-label="业务指标">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-copy">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.detail }}</small>
        </div>
        <div class="metric-icon" :class="metric.tone">
          <component :is="metric.icon" :size="22" />
        </div>
      </article>
    </section>

    <section class="overview-grid">
      <article class="campus-panel">
        <div class="section-heading">
          <div>
            <span class="eyebrow">当前项目</span>
            <h3>示范科技园区</h3>
          </div>
          <el-button text @click="openMap">
            打开项目
            <ArrowRight :size="16" />
          </el-button>
        </div>
        <button class="campus-visual" type="button" aria-label="打开示范科技园区" @click="openMap">
          <img src="/assets/demo-campus.png" alt="示范科技园区鸟瞰效果图" />
          <span class="visual-overlay">
            <span class="visual-label">本地示范场景</span>
            <strong>5 栋楼宇 · 1 个精细对象</strong>
            <small>图片用于界面展示，不代表真实工程数据</small>
          </span>
        </button>
      </article>

      <aside class="task-panel">
        <div class="section-heading">
          <div>
            <span class="eyebrow">近期任务</span>
            <h3>待办与校准</h3>
          </div>
          <el-button text @click="showPendingMessage">全部</el-button>
        </div>
        <div class="task-list">
          <div v-for="task in tasks" :key="task.name" class="task-row">
            <span>{{ task.name }}</span>
            <el-tag :type="task.type" effect="plain">{{ task.state }}</el-tag>
          </div>
        </div>
        <div class="notice">
          只有用户主动圈选或标注的建筑，才进入平台自有资源管理范围。
        </div>
      </aside>
    </section>

    <section class="project-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">项目清单</span>
          <h3>最近访问</h3>
        </div>
        <el-button @click="router.push('/projects')">管理项目</el-button>
      </div>
      <el-table :data="projects">
        <el-table-column prop="name" label="项目名称" min-width="180" />
        <el-table-column prop="buildings" label="楼宇数" width="100" />
        <el-table-column prop="detail" label="精细对象" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === '已发布' ? 'success' : 'warning'" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新" min-width="120" />
        <el-table-column label="操作" width="100" align="right">
          <template #default>
            <el-button link type="primary" @click="openMap">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 20px;
}

.page-heading,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.page-heading h2,
.section-heading h3 {
  margin: 3px 0 0;
}

.page-heading h2 {
  font-size: 25px;
}

.page-heading p {
  margin: 5px 0 0;
  color: var(--jw-muted);
}

.section-heading h3 {
  font-size: 17px;
}

.eyebrow {
  color: var(--jw-accent);
  font-size: 11px;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card,
.campus-panel,
.task-panel,
.project-section {
  border: 1px solid var(--jw-border);
  border-radius: var(--jw-radius);
  background: var(--jw-surface);
}

.metric-card {
  display: flex;
  min-height: 138px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px;
}

.metric-copy {
  display: grid;
}

.metric-copy > span,
.metric-copy small {
  color: var(--jw-muted);
  font-size: 12px;
}

.metric-copy strong {
  margin: 7px 0 2px;
  font-size: 30px;
  line-height: 1.1;
}

.metric-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 7px;
}

.metric-icon.teal {
  background: #e7f3f1;
  color: #267d76;
}

.metric-icon.blue {
  background: #e8f0f5;
  color: #3f7391;
}

.metric-icon.amber {
  background: #f7eee4;
  color: #a6652f;
}

.metric-icon.red {
  background: #f7eaea;
  color: #a84949;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(290px, 0.85fr);
  gap: 16px;
}

.campus-panel,
.task-panel,
.project-section {
  padding: 18px;
}

.campus-visual {
  position: relative;
  display: block;
  width: 100%;
  height: 330px;
  margin-top: 14px;
  overflow: hidden;
  border: 0;
  border-radius: 6px;
  background: #cbd9d9;
  color: #fff;
  text-align: left;
}

.campus-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 220ms ease;
}

.campus-visual:hover img {
  transform: scale(1.015);
}

.visual-overlay {
  position: absolute;
  inset: auto 0 0;
  display: grid;
  gap: 3px;
  padding: 58px 20px 18px;
  background: linear-gradient(transparent, rgb(13 35 41 / 86%));
}

.visual-label {
  width: fit-content;
  padding: 3px 7px;
  border: 1px solid rgb(255 255 255 / 35%);
  border-radius: 4px;
  font-size: 11px;
}

.visual-overlay strong {
  font-size: 18px;
}

.visual-overlay small {
  color: #cae0e2;
}

.task-list {
  margin-top: 12px;
}

.task-row {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #edf1f2;
  font-size: 13px;
}

.notice {
  margin-top: 18px;
  padding: 12px;
  border-left: 3px solid #c18246;
  background: #faf5ef;
  color: #72583f;
  font-size: 12px;
}

.project-section .el-table {
  margin-top: 12px;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .campus-visual {
    height: 280px;
  }
}

@media (max-width: 580px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .campus-visual {
    height: 240px;
  }
}
</style>
