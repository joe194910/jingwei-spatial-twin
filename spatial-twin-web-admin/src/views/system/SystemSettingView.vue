<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Map, RefreshCw, Save, ShieldAlert } from '@lucide/vue'
import { ElMessage } from 'element-plus'

import { getMapProviderConfig, updateMapProviderConfig } from '@/api/map-config'
import { listSettings, updateSetting } from '@/api/system'
import type { MapProviderAdminConfig, MapProviderConfigUpdateRequest } from '@/map/types'
import { ApiError } from '@/types/api'
import type { SettingItem } from '@/types/system'

// 敏感配置只允许覆盖写入，页面不会保存或回显后端已有明文。
const loading = ref(false)
const loadError = ref('')
const settings = ref<SettingItem[]>([])
const drafts = reactive<Record<string, string | number>>({})
const savingKeys = reactive(new Set<string>())
const mapConfig = ref<MapProviderAdminConfig | null>(null)
const mapSaving = ref(false)
const mapDraft = reactive<MapProviderConfigUpdateRequest>({
  enabled: false,
  apiVersion: '2.0',
  webKey: '',
  clearWebKey: false,
  securityJsCode: '',
  clearSecurityJsCode: false,
  mapStyle: '',
  viewMode: '3D',
  defaultPitch: 45,
  defaultRotation: 0,
  minZoom: 3,
  maxZoom: 20,
  showScale: true,
  showToolbar: true,
  showControlBar: true,
})
const mapSaveDisabled = computed(() => {
  const config = mapConfig.value
  return Boolean(
    config &&
      !config.encryptionReady &&
      (!config.webKeyConfigured || !config.securityJsCodeConfigured),
  )
})

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function initializeDrafts(items: SettingItem[]) {
  for (const key of Object.keys(drafts)) {
    delete drafts[key]
  }
  for (const item of items) {
    drafts[item.key] = item.sensitive
      ? ''
      : item.valueType === 'INTEGER'
        ? Number(item.value)
        : item.value
  }
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [data, providerConfig] = await Promise.all([
      listSettings(),
      getMapProviderConfig(),
    ])
    settings.value = data
    initializeDrafts(data)
    mapConfig.value = providerConfig
    Object.assign(mapDraft, {
      enabled: providerConfig.enabled,
      apiVersion: providerConfig.apiVersion,
      webKey: '',
      clearWebKey: false,
      securityJsCode: '',
      clearSecurityJsCode: false,
      mapStyle: providerConfig.mapStyle || '',
      viewMode: providerConfig.viewMode,
      defaultPitch: providerConfig.defaultPitch,
      defaultRotation: providerConfig.defaultRotation,
      minZoom: providerConfig.minZoom,
      maxZoom: providerConfig.maxZoom,
      showScale: providerConfig.showScale,
      showToolbar: providerConfig.showToolbar,
      showControlBar: providerConfig.showControlBar,
    })
  } catch (error) {
    loadError.value = errorMessage(error, '系统配置加载失败')
  } finally {
    loading.value = false
  }
}

async function saveMapConfig() {
  if (mapDraft.minZoom > mapDraft.maxZoom) {
    ElMessage.warning('最小缩放级别不能大于最大缩放级别')
    return
  }
  if (
    mapDraft.enabled &&
    (!mapConfig.value?.webKeyConfigured || mapDraft.clearWebKey) &&
    !mapDraft.webKey.trim()
  ) {
    ElMessage.warning('启用在线地图前请配置高德Web端Key')
    return
  }
  if (
    mapDraft.enabled &&
    (!mapConfig.value?.securityJsCodeConfigured || mapDraft.clearSecurityJsCode) &&
    !mapDraft.securityJsCode.trim()
  ) {
    ElMessage.warning('启用在线地图前请配置高德安全密钥')
    return
  }

  mapSaving.value = true
  try {
    await updateMapProviderConfig({
      ...mapDraft,
      webKey: mapDraft.webKey.trim(),
      securityJsCode: mapDraft.securityJsCode.trim(),
      mapStyle: mapDraft.mapStyle.trim(),
    })
    ElMessage.success('地图底座配置修改成功')
    await loadData()
  } catch (error) {
    ElMessage.error(errorMessage(error, '地图底座配置保存失败'))
  } finally {
    mapSaving.value = false
  }
}

async function saveSetting(setting: SettingItem) {
  const value = String(drafts[setting.key] ?? '').trim()
  if (!value) {
    ElMessage.warning(setting.sensitive ? '请输入新的敏感配置值' : '请输入配置值')
    return
  }

  savingKeys.add(setting.key)
  try {
    await updateSetting(setting.key, value)
    ElMessage.success('系统配置修改成功')
    await loadData()
  } catch (error) {
    ElMessage.error(errorMessage(error, '系统配置保存失败'))
  } finally {
    savingKeys.delete(setting.key)
  }
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="management-heading">
      <div>
        <span class="management-eyebrow">安全与平台参数</span>
        <h2>系统配置</h2>
        <p>仅超级管理员可访问。敏感配置只可覆盖更新，后端不会返回其明文。</p>
      </div>
      <el-button :loading="loading" @click="loadData">
        <RefreshCw :size="16" />
        刷新
      </el-button>
    </header>

    <el-alert
      title="万能登录密码具有高风险，仅应作为受控运维能力使用；设置后不会在页面回显。"
      type="warning"
      show-icon
      :closable="false"
      class="management-alert"
    >
      <template #icon><ShieldAlert :size="18" /></template>
    </el-alert>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      class="management-alert"
    >
      <template #default>
        <el-button link type="primary" @click="loadData">重新加载</el-button>
      </template>
    </el-alert>

    <section v-if="mapConfig" class="map-config-section" aria-label="地图底座配置">
      <div class="map-config-heading">
        <div>
          <span class="map-config-icon"><Map :size="18" /></span>
          <div>
            <h3>地图底座</h3>
            <p>在线地图不可用时，首页自动切换为经纬离线底座。</p>
          </div>
        </div>
        <el-switch
          v-model="mapDraft.enabled"
          active-text="启用高德"
          inactive-text="仅离线底座"
        />
      </div>

      <el-alert
        v-if="!mapConfig.encryptionReady"
        title="服务器尚未配置 JW_MAP_CONFIG_ENCRYPTION_KEY，设置新凭证前请先配置环境变量。"
        type="warning"
        :closable="false"
        show-icon
        class="map-config-alert"
      />

      <el-form label-position="top" class="map-config-form">
        <el-form-item label="高德 JS API 版本">
          <el-input v-model="mapDraft.apiVersion" disabled />
        </el-form-item>
        <el-form-item label="视图模式">
          <el-radio-group v-model="mapDraft.viewMode">
            <el-radio-button value="2D">2D</el-radio-button>
            <el-radio-button value="3D">3D</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="高德 Web端 Key">
          <el-input
            v-model="mapDraft.webKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="
              mapConfig.webKeyConfigured ? '已配置，留空保持不变' : '请输入高德Web端Key'
            "
          />
          <el-checkbox v-model="mapDraft.clearWebKey">清除已保存的Key</el-checkbox>
        </el-form-item>
        <el-form-item label="高德安全密钥">
          <el-input
            v-model="mapDraft.securityJsCode"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="
              mapConfig.securityJsCodeConfigured ? '已配置，留空保持不变' : '请输入安全密钥'
            "
          />
          <el-checkbox v-model="mapDraft.clearSecurityJsCode">清除已保存的安全密钥</el-checkbox>
        </el-form-item>
        <el-form-item label="地图样式">
          <el-input
            v-model="mapDraft.mapStyle"
            maxlength="160"
            placeholder="留空使用清晰标准底图，例如 amap://styles/normal"
          />
        </el-form-item>
        <el-form-item label="默认俯仰角">
          <el-input-number
            v-model="mapDraft.defaultPitch"
            :min="0"
            :max="83"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="默认旋转角">
          <el-input-number
            v-model="mapDraft.defaultRotation"
            :min="0"
            :max="359"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="缩放范围">
          <div class="zoom-range">
            <el-input-number
              v-model="mapDraft.minZoom"
              :min="2"
              :max="20"
              controls-position="right"
            />
            <span>至</span>
            <el-input-number
              v-model="mapDraft.maxZoom"
              :min="2"
              :max="20"
              controls-position="right"
            />
          </div>
        </el-form-item>
      </el-form>

      <div class="map-control-options">
        <el-checkbox v-model="mapDraft.showScale">比例尺</el-checkbox>
        <el-checkbox v-model="mapDraft.showToolbar">缩放工具</el-checkbox>
        <el-checkbox v-model="mapDraft.showControlBar" :disabled="mapDraft.viewMode !== '3D'">
          旋转与倾斜控件
        </el-checkbox>
      </div>

      <div class="map-config-actions">
        <span>凭证仅保存密文，页面不会回显已有值。</span>
        <el-button
          type="primary"
          :loading="mapSaving"
          :disabled="mapSaveDisabled"
          @click="saveMapConfig"
        >
          <Save :size="16" />
          保存地图配置
        </el-button>
      </div>
    </section>

    <div v-loading="loading" class="setting-list">
      <article v-for="setting in settings" :key="setting.key" class="setting-row">
        <div class="setting-copy">
          <div class="setting-title">
            <strong>{{ setting.description }}</strong>
            <el-tag v-if="setting.sensitive" type="warning" effect="plain">敏感</el-tag>
          </div>
          <code>{{ setting.key }}</code>
        </div>

        <div class="setting-control">
          <el-select
            v-if="setting.valueType === 'BOOLEAN'"
            v-model="drafts[setting.key]"
            aria-label="布尔配置值"
          >
            <el-option label="开启" value="true" />
            <el-option label="关闭" value="false" />
          </el-select>
          <el-input-number
            v-else-if="setting.valueType === 'INTEGER'"
            v-model="drafts[setting.key]"
            :min="0"
            :max="1440"
            controls-position="right"
            aria-label="整数配置值"
          />
          <el-input
            v-else
            v-model="drafts[setting.key]"
            :type="setting.sensitive ? 'password' : 'text'"
            :show-password="setting.sensitive"
            maxlength="128"
            autocomplete="new-password"
            :placeholder="setting.sensitive ? '输入新值进行覆盖，当前值不回显' : '请输入配置值'"
          />
          <el-button
            type="primary"
            :loading="savingKeys.has(setting.key)"
            @click="saveSetting(setting)"
          >
            <Save :size="16" />
            保存
          </el-button>
        </div>
      </article>
      <el-empty v-if="!loading && settings.length === 0 && !loadError" description="暂无系统配置" />
    </div>
  </section>
</template>

<style scoped>
.map-config-section {
  margin-bottom: 22px;
  padding: 20px 0 22px;
  border-top: 1px solid #dfe8e6;
  border-bottom: 1px solid #dfe8e6;
}

.map-config-heading,
.map-config-heading > div,
.map-config-actions,
.zoom-range {
  display: flex;
  align-items: center;
}

.map-config-heading {
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.map-config-heading > div {
  min-width: 0;
  gap: 10px;
}

.map-config-heading h3,
.map-config-heading p {
  margin: 0;
}

.map-config-heading h3 {
  font-size: 16px;
}

.map-config-heading p,
.map-config-actions span {
  color: var(--jw-muted);
  font-size: 12px;
}

.map-config-icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 6px;
  background: #e1efec;
  color: #17645f;
}

.map-config-alert {
  margin-bottom: 16px;
}

.map-config-form {
  display: grid;
  gap: 0 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.map-config-form :deep(.el-input-number) {
  width: 100%;
}

.zoom-range {
  gap: 8px;
}

.zoom-range span {
  color: var(--jw-muted);
  font-size: 12px;
}

.map-control-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 22px;
  padding: 4px 0 18px;
}

.map-config-actions {
  justify-content: space-between;
  gap: 16px;
}

@media (max-width: 1000px) {
  .map-config-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .map-config-heading,
  .map-config-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .map-config-form {
    grid-template-columns: 1fr;
  }
}
</style>
