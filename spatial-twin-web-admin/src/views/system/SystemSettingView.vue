<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RefreshCw, Save, ShieldAlert } from '@lucide/vue'
import { ElMessage } from 'element-plus'

import { listSettings, updateSetting } from '@/api/system'
import { ApiError } from '@/types/api'
import type { SettingItem } from '@/types/system'

// 敏感配置只允许覆盖写入，页面不会保存或回显后端已有明文。
const loading = ref(false)
const loadError = ref('')
const settings = ref<SettingItem[]>([])
const drafts = reactive<Record<string, string | number>>({})
const savingKeys = reactive(new Set<string>())

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
    const data = await listSettings()
    settings.value = data
    initializeDrafts(data)
  } catch (error) {
    loadError.value = errorMessage(error, '系统配置加载失败')
  } finally {
    loading.value = false
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
