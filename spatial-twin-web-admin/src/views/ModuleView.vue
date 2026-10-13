<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Box,
  Building2,
  Camera,
  CheckCircle2,
  Download,
  FileCheck2,
  Gauge,
  Plus,
  RefreshCw,
  Route,
  Save,
  Upload,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'

interface Metric {
  label: string
  value: string
  detail?: string
}

interface TableColumn {
  prop: string
  label: string
  width?: number
}

interface ModuleConfig {
  eyebrow: string
  description: string
  primaryAction: string
  metrics: Metric[]
  columns: TableColumn[]
  rows: Record<string, string>[]
  note: string
}

const route = useRoute()
const router = useRouter()
const searchKeyword = ref('')
const detailLoaded = ref(false)
const selectedFloor = ref('6F')
const selectedRoom = ref('研发部')
const quality = ref('自动')

const configs: Record<string, ModuleConfig> = {
  projects: {
    eyebrow: '空间项目',
    description: '管理园区边界、默认地图区域和项目成员范围。',
    primaryAction: '创建项目',
    metrics: [
      { label: '全部项目', value: '02' },
      { label: '草稿项目', value: '01' },
      { label: '已发布', value: '01' },
      { label: '标注楼宇', value: '13' },
    ],
    columns: [
      { prop: 'name', label: '项目名称' },
      { prop: 'buildings', label: '楼宇' },
      { prop: 'detail', label: '精细对象' },
      { prop: 'status', label: '状态' },
      { prop: 'updatedAt', label: '最近更新' },
    ],
    rows: [
      { name: '示范科技园区', buildings: '5', detail: '1', status: '草稿', updatedAt: '本地演示' },
      { name: '智慧住宅样板', buildings: '8', detail: '2', status: '已发布', updatedAt: '本地演示' },
    ],
    note: '项目坐标、边界和成员权限将在后端项目域完成后持久化。',
  },
  assets: {
    eyebrow: '资源管理',
    description: '模型原件、优化产物和楼层图纸分开管理。',
    primaryAction: '上传模型或图纸',
    metrics: [
      { label: 'GLB 模型', value: '16' },
      { label: '楼层图纸', value: '14' },
      { label: '建筑模板', value: '08' },
      { label: '待处理文件', value: '02' },
    ],
    columns: [
      { prop: 'name', label: '资产名称' },
      { prop: 'type', label: '类型' },
      { prop: 'size', label: '大小' },
      { prop: 'state', label: '处理状态' },
      { prop: 'target', label: '关联对象' },
    ],
    rows: [
      { name: '研发中心-A-v2.glb', type: '精细外观', size: '32 MB', state: '已优化', target: 'A 栋' },
      { name: '办公楼-template-03.glb', type: '标准模板', size: '6 MB', state: '已优化', target: 'B 栋' },
      { name: 'A栋-6F-plan.pdf', type: '楼层图纸', size: '2 MB', state: '待配准', target: 'A 栋 6F' },
    ],
    note: '当前清单是界面样例。正式导入必须保留原件、校验格式与来源。',
  },
  release: {
    eyebrow: '场景交付',
    description: '将草稿校验后生成不可变的发布版本。',
    primaryAction: '发布当前草稿',
    metrics: [
      { label: '当前草稿', value: '#12' },
      { label: '模拟变更', value: '03' },
      { label: '冲突项', value: '00' },
      { label: '待核验资料', value: '01' },
    ],
    columns: [
      { prop: 'version', label: '版本' },
      { prop: 'state', label: '状态' },
      { prop: 'changes', label: '变更数' },
      { prop: 'publisher', label: '发布人' },
      { prop: 'time', label: '时间' },
    ],
    rows: [
      { version: 'Draft #12', state: '待发布', changes: '3', publisher: '空间管理员', time: '本地演示' },
      { version: 'v0.3.1', state: '已发布', changes: '5', publisher: '空间管理员', time: '历史样例' },
      { version: 'v0.3.0', state: '已发布', changes: '8', publisher: '空间管理员', time: '历史样例' },
    ],
    note: '发布前必须检查缺失资产、冲突绑定、权限和图纸核验状态。',
  },
  audit: {
    eyebrow: '安全留痕',
    description: '查询登录、下载、模型替换和发布等敏感操作。',
    primaryAction: '导出审计',
    metrics: [
      { label: '今日操作', value: '03' },
      { label: '成功', value: '02' },
      { label: '已拒绝', value: '01' },
      { label: '异常请求', value: '00' },
    ],
    columns: [
      { prop: 'time', label: '时间' },
      { prop: 'account', label: '账号' },
      { prop: 'action', label: '操作' },
      { prop: 'target', label: '对象' },
      { prop: 'result', label: '结果' },
      { prop: 'requestId', label: '请求 ID' },
    ],
    rows: [
      { time: '演示 10:35', account: 'admin', action: '替换模型', target: 'Building-001', result: '成功', requestId: 'req-1001' },
      { time: '演示 09:10', account: 'viewer_demo', action: '下载管线资料', target: 'Layer-019', result: '拒绝', requestId: 'req-1002' },
      { time: '演示 08:50', account: 'admin', action: '登录', target: '会话', result: '成功', requestId: 'req-1003' },
    ],
    note: '审计导出需要独立权限，本轮仅展示数据结构和查询入口。',
  },
}

const moduleKey = computed(() => String(route.meta.moduleKey ?? 'projects'))
const config = computed(() => configs[moduleKey.value] ?? configs.projects)
const filteredRows = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return config.value.rows
  return config.value.rows.filter((row) =>
    Object.values(row).some((cell) => cell.toLowerCase().includes(keyword)),
  )
})

function showAction(action: string) {
  ElMessage.info(`${action}为当前前端交互演示，尚未连接业务接口。`)
}

function openPrimaryAction() {
  showAction(config.value.primaryAction)
}
</script>

<template>
  <div v-if="moduleKey === 'building'" class="special-page">
    <section class="module-heading">
      <div>
        <span class="eyebrow">精细对象会话</span>
        <h2>A 栋研发中心 · 精细楼宇</h2>
        <p>一次只允许一个楼宇进入精细模式，退出后释放无引用资源。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="router.push('/map')"><ArrowLeft :size="16" />返回园区</el-button>
        <el-button type="primary" @click="detailLoaded = !detailLoaded">
          <Box :size="16" />{{ detailLoaded ? '退出精细模式' : '加载精细模型' }}
        </el-button>
      </div>
    </section>
    <section class="building-workspace">
      <aside class="side-panel">
        <h3>楼宇层级</h3>
        <button class="tree-row active" type="button"><Building2 :size="16" />A 栋研发中心</button>
        <button
          v-for="floor in ['8F', '7F', '6F', '5F', '4F', '3F', '2F', '1F']"
          :key="floor"
          class="tree-row"
          :class="{ active: selectedFloor === floor }"
          type="button"
          @click="selectedFloor = floor"
        >
          {{ floor }} · {{ floor === '6F' ? '研发楼层' : '空间楼层' }}
        </button>
      </aside>
      <div class="building-stage">
        <div class="tower">
          <span v-for="floor in 8" :key="floor" />
        </div>
        <div class="stage-state">
          {{ detailLoaded ? '精细资源已进入前端模拟状态' : '标准写实占位模型 · 未加载精细资源' }}
        </div>
        <div class="stage-tags"><span>当前 {{ selectedFloor }}</span><span>可替换 GLB</span><span>按需加载</span></div>
      </div>
      <aside class="side-panel details">
        <h3>建筑信息</h3>
        <dl>
          <dt>建筑 ID</dt><dd>Building-001</dd>
          <dt>当前楼层</dt><dd>{{ selectedFloor }}</dd>
          <dt>模型版本</dt><dd>v2.0 / 标准</dd>
        </dl>
        <el-button class="wide-button" @click="router.push('/assets')"><Upload :size="16" />上传 GLB 替换</el-button>
        <el-button class="wide-button" type="primary" @click="router.push('/floor')">打开楼层编辑</el-button>
      </aside>
    </section>
  </div>

  <div v-else-if="moduleKey === 'floor'" class="special-page">
    <section class="module-heading">
      <div>
        <span class="eyebrow">二维空间资料</span>
        <h2>A 栋 · {{ selectedFloor }} 空间管理</h2>
        <p>房间、部门、设备和路线按楼层独立维护。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="router.push('/building')"><ArrowLeft :size="16" />返回楼宇</el-button>
        <el-button @click="showAction('导入 PDF 或图片')"><Upload :size="16" />导入图纸</el-button>
        <el-button type="primary" @click="showAction('保存楼层草稿')"><Save :size="16" />保存草稿</el-button>
      </div>
    </section>
    <section class="floor-workspace">
      <aside class="side-panel">
        <h3>楼层与工具</h3>
        <el-select v-model="selectedFloor" style="width: 100%">
          <el-option v-for="floor in ['8F', '7F', '6F', '5F', '4F']" :key="floor" :label="floor" :value="floor" />
        </el-select>
        <button class="tool-row" type="button" @click="showAction('房间绘制')"><Plus :size="16" />绘制房间</button>
        <button class="tool-row" type="button" @click="showAction('设备点位')"><Camera :size="16" />放置摄像头</button>
        <button class="tool-row" type="button" @click="showAction('专业路线')"><Route :size="16" />绘制路线</button>
      </aside>
      <div class="floor-plan">
        <button class="room room-large" type="button" @click="selectedRoom = '研发部'">研发部<br /><small>32 人 / 180㎡</small></button>
        <button class="room room-green" type="button" @click="selectedRoom = '行政部'">行政部<br /><small>18 人 / 210㎡</small></button>
        <button class="room room-amber" type="button" @click="selectedRoom = '会议室'">会议室<br /><small>20 座</small></button>
        <div class="corridor">公共走廊 · 逃生路线未配置</div>
        <button class="room room-blue" type="button" @click="selectedRoom = '技术支持'">技术支持<br /><small>12 人</small></button>
        <button class="room" type="button" @click="selectedRoom = '设备间'">设备间<br /><small>暖通</small></button>
      </div>
      <aside class="side-panel details">
        <h3>空间属性</h3>
        <el-form label-position="top">
          <el-form-item label="选中空间"><el-input v-model="selectedRoom" /></el-form-item>
          <el-form-item label="空间 ID"><el-input model-value="Space-6F-001" disabled /></el-form-item>
          <el-form-item label="人数统计"><el-input-number :model-value="32" :min="0" /></el-form-item>
          <el-form-item label="专业图层">
            <el-select model-value="房间布局" style="width: 100%">
              <el-option label="房间布局" value="房间布局" />
              <el-option label="消防设施" value="消防设施" />
              <el-option label="机电管线" value="机电管线" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="inline-note">工程路线和疏散资料必须经过专业核验。</div>
      </aside>
    </section>
  </div>

  <div v-else-if="moduleKey === 'settings'" class="special-page">
    <section class="module-heading">
      <div>
        <span class="eyebrow">平台配置</span>
        <h2>系统与安全设置</h2>
        <p>敏感配置仅展示字段，不保存、不回显真实密钥。</p>
      </div>
      <el-button type="primary" @click="showAction('保存系统设置')"><Save :size="16" />保存设置</el-button>
    </section>
    <section class="settings-grid">
      <div class="settings-section">
        <h3>登录与安全策略</h3>
        <el-form label-position="top">
          <el-form-item label="开放用户注册">
            <el-select model-value="关闭" style="width: 100%">
              <el-option label="关闭（推荐默认）" value="关闭" />
              <el-option label="开启（需配套安全措施）" value="开启" />
            </el-select>
          </el-form-item>
          <el-form-item label="登录失败限制"><el-input-number :model-value="5" :min="3" /></el-form-item>
          <el-form-item label="会话超时（分钟）"><el-input-number :model-value="120" :min="10" /></el-form-item>
          <el-checkbox :model-value="true">审计敏感操作</el-checkbox>
        </el-form>
      </div>
      <div class="settings-section">
        <h3>地图与存储</h3>
        <el-form label-position="top">
          <el-form-item label="地图提供方"><el-input model-value="高德 JS API 2.0 适配层" disabled /></el-form-item>
          <el-form-item label="高德地图 Key"><el-input type="password" placeholder="通过环境变量配置" /></el-form-item>
          <el-form-item label="模型上传上限（MB）"><el-input-number :model-value="200" :min="1" /></el-form-item>
          <el-form-item label="文件存储方式">
            <el-select model-value="MinIO 对象存储" style="width: 100%" disabled>
              <el-option label="MinIO 对象存储" value="MinIO 对象存储" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
    </section>
  </div>

  <div v-else-if="moduleKey === 'performance'" class="special-page">
    <section class="module-heading">
      <div>
        <span class="eyebrow">浏览器资源调度</span>
        <h2>性能调度与资源监控</h2>
        <p>本页数据为原型模拟值，后续接入 Three.js 实际统计。</p>
      </div>
      <el-button @click="showAction('刷新性能采样')"><RefreshCw :size="16" />刷新采样</el-button>
    </section>
    <section class="metric-grid">
      <article><Gauge :size="20" /><span>FPS（模拟）</span><strong>48</strong><small>标准画质</small></article>
      <article><Building2 :size="20" /><span>可见自有建筑</span><strong>3</strong><small>当前视野</small></article>
      <article><Box :size="20" /><span>精细加载</span><strong>0</strong><small>最多 1 个</small></article>
      <article><Download :size="20" /><span>缓存估算</span><strong>86 MB</strong><small>模拟统计</small></article>
    </section>
    <section class="performance-grid">
      <div class="settings-section">
        <h3>调度优先级</h3>
        <label>已选楼宇精细资源 <el-progress :percentage="95" /></label>
        <label>当前视野标准模型 <el-progress :percentage="65" /></label>
        <label>相邻区域预加载 <el-progress :percentage="15" /></label>
      </div>
      <div class="settings-section">
        <h3>画质设置</h3>
        <el-select v-model="quality" style="width: 100%">
          <el-option label="自动（推荐）" value="自动" />
          <el-option label="轻量" value="轻量" />
          <el-option label="平衡" value="平衡" />
          <el-option label="高画质" value="高画质" />
        </el-select>
        <el-checkbox :model-value="true">只允许一栋楼进入精细模式</el-checkbox>
        <el-checkbox :model-value="true">退出后释放无引用模型资源</el-checkbox>
        <el-checkbox :model-value="true">拖动地图时减少高精下载</el-checkbox>
      </div>
    </section>
  </div>

  <div v-else class="module-page">
    <section class="module-heading">
      <div>
        <span class="eyebrow">{{ config.eyebrow }}</span>
        <h2>{{ route.meta.title }}</h2>
        <p>{{ config.description }}</p>
      </div>
      <el-button type="primary" @click="openPrimaryAction">
        <Upload v-if="moduleKey === 'assets'" :size="16" />
        <FileCheck2 v-else-if="moduleKey === 'release'" :size="16" />
        <Download v-else-if="moduleKey === 'audit'" :size="16" />
        <Plus v-else :size="16" />
        {{ config.primaryAction }}
      </el-button>
    </section>

    <section class="metric-grid">
      <article v-for="metric in config.metrics" :key="metric.label">
        <CheckCircle2 :size="20" />
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.detail ?? '演示数据' }}</small>
      </article>
    </section>

    <section class="data-section">
      <div class="table-tools">
        <el-input v-model="searchKeyword" clearable placeholder="搜索当前列表" />
        <el-button @click="showAction('刷新列表')"><RefreshCw :size="16" />刷新</el-button>
      </div>
      <el-table :data="filteredRows">
        <el-table-column
          v-for="column in config.columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          min-width="120"
        />
        <el-table-column label="操作" width="100" align="right">
          <template #default><el-button link type="primary" @click="showAction('查看详情')">查看</el-button></template>
        </el-table-column>
      </el-table>
      <div class="inline-note">{{ config.note }}</div>
    </section>
  </div>
</template>

<style scoped>
.module-page,
.special-page {
  display: grid;
  gap: 18px;
}

.module-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.module-heading h2 {
  margin: 3px 0;
  font-size: 23px;
}

.module-heading p {
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
  gap: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-grid article,
.data-section,
.settings-section {
  border: 1px solid var(--jw-border);
  border-radius: var(--jw-radius);
  background: var(--jw-surface);
}

.metric-grid article {
  position: relative;
  display: grid;
  min-height: 130px;
  align-content: center;
  padding: 18px;
}

.metric-grid article > svg {
  position: absolute;
  top: 18px;
  right: 18px;
  color: #6c9995;
}

.metric-grid span,
.metric-grid small {
  color: var(--jw-muted);
  font-size: 12px;
}

.metric-grid strong {
  margin: 5px 0;
  font-size: 27px;
}

.data-section {
  padding: 16px;
}

.table-tools {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

.table-tools .el-input {
  width: min(280px, 100%);
}

.inline-note {
  margin-top: 14px;
  padding: 11px 12px;
  border-left: 3px solid #b8783d;
  background: #faf5ef;
  color: #765b42;
  font-size: 12px;
}

.building-workspace,
.floor-workspace {
  display: grid;
  min-height: 640px;
  grid-template-columns: 220px minmax(420px, 1fr) 260px;
  overflow: hidden;
  border: 1px solid var(--jw-border);
  border-radius: var(--jw-radius);
  background: var(--jw-surface);
}

.side-panel {
  padding: 16px;
  border-right: 1px solid var(--jw-border);
}

.side-panel.details {
  border-right: 0;
  border-left: 1px solid var(--jw-border);
}

.side-panel h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.tree-row,
.tool-row {
  display: flex;
  width: 100%;
  min-height: 39px;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--jw-text);
  text-align: left;
}

.tree-row:hover,
.tool-row:hover,
.tree-row.active {
  background: #e8f3f1;
  color: #17645f;
}

.tool-row:first-of-type {
  margin-top: 14px;
}

.building-stage {
  position: relative;
  display: grid;
  overflow: hidden;
  place-items: center;
  background:
    linear-gradient(rgb(255 255 255 / 35%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(255 255 255 / 35%) 1px, transparent 1px),
    #dce9e8;
  background-size: 32px 32px;
}

.tower {
  width: 230px;
  padding: 13px;
  border: 1px solid #698a91;
  background: #486d79;
  box-shadow: 28px 28px 0 rgb(34 75 85 / 18%);
  transform: perspective(700px) rotateX(5deg) rotateY(-12deg);
}

.tower span {
  display: block;
  height: 42px;
  margin: 4px 0;
  border: 1px solid #a7c1c4;
  background: repeating-linear-gradient(90deg, #7ba0a6 0 26px, #b9d0cf 26px 34px);
}

.stage-state,
.stage-tags {
  position: absolute;
  left: 15px;
}

.stage-state {
  top: 15px;
  padding: 8px 10px;
  border-radius: 5px;
  background: rgb(255 255 255 / 88%);
  color: #48636b;
  font-size: 11px;
}

.stage-tags {
  bottom: 15px;
  display: flex;
  gap: 6px;
}

.stage-tags span {
  padding: 5px 8px;
  border: 1px solid #aac5c3;
  border-radius: 4px;
  background: rgb(255 255 255 / 85%);
  color: #326d68;
  font-size: 11px;
}

dl {
  margin: 0 0 16px;
}

dt {
  margin-top: 12px;
  color: var(--jw-muted);
  font-size: 11px;
}

dd {
  margin: 2px 0 0;
  font-weight: 700;
}

.wide-button {
  width: 100%;
  margin: 0 0 9px;
}

.wide-button + .wide-button {
  margin-left: 0;
}

.floor-plan {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  grid-template-rows: 1fr 0.7fr 44px 1fr;
  gap: 9px;
  padding: 24px;
  background: #eef3f3;
}

.room {
  border: 2px solid #6c9295;
  border-radius: 4px;
  background: #d7e7e6;
  color: #294b53;
  font-weight: 700;
}

.room:hover {
  box-shadow: inset 0 0 0 3px rgb(255 255 255 / 70%);
}

.room-large {
  grid-row: span 2;
}

.room-green {
  border-color: #6d9a7d;
  background: #dceadd;
}

.room-amber {
  border-color: #b58a54;
  background: #eee2cf;
}

.room-blue {
  border-color: #6288a0;
  background: #d9e5ec;
}

.corridor {
  display: grid;
  grid-column: 1 / -1;
  place-items: center;
  border: 1px dashed #9daeb2;
  background: #fff;
  color: #7a6161;
  font-size: 11px;
}

.settings-grid,
.performance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.settings-section {
  padding: 20px;
}

.settings-section h3 {
  margin: 0 0 17px;
  font-size: 16px;
}

.performance-grid label {
  display: block;
  margin: 16px 0;
  color: #546b73;
  font-size: 12px;
}

.performance-grid .el-checkbox {
  display: flex;
  margin: 14px 0;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .building-workspace,
  .floor-workspace {
    grid-template-columns: 200px minmax(400px, 1fr);
  }

  .side-panel.details {
    grid-column: 1 / -1;
    border-top: 1px solid var(--jw-border);
    border-left: 0;
  }
}

@media (max-width: 720px) {
  .module-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .heading-actions {
    flex-wrap: wrap;
  }

  .metric-grid,
  .settings-grid,
  .performance-grid,
  .building-workspace,
  .floor-workspace {
    grid-template-columns: 1fr;
  }

  .side-panel {
    border-right: 0;
    border-bottom: 1px solid var(--jw-border);
  }

  .building-stage {
    min-height: 480px;
  }

  .floor-plan {
    min-height: 480px;
  }

  .table-tools {
    justify-content: stretch;
  }
}
</style>
