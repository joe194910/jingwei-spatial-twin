<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { getMapRuntimeConfig } from '@/api/map-config'
import { AmapMapAdapter } from '@/map/adapters/AmapMapAdapter'
import { JingweiOfflineMapAdapter } from '@/map/adapters/JingweiOfflineMapAdapter'
import type { MapAdapter, MapProviderState, OverviewProjectMarker } from '@/map/types'

interface OverviewMapProps {
  /** 当前用户可见的项目标注集合。 */
  projects: OverviewProjectMarker[]
  /** 用于同步列表与地图高亮状态的项目编号。 */
  selectedProjectId: string | null
}

interface OverviewMapEmits {
  /** 用户从地图标注选择项目时通知上层页面。 */
  select: [projectId: string]
  /** 地图供应商或降级状态变化时通知上层页面。 */
  providerState: [state: MapProviderState]
}

const props = defineProps<OverviewMapProps>()
const emit = defineEmits<OverviewMapEmits>()

const mapHost = ref<HTMLDivElement>()
let mapAdapter: MapAdapter | null = null
let initializationVersion = 0

async function mountAdapter(adapter: MapAdapter) {
  if (!mapHost.value) {
    return
  }
  await adapter.mount(mapHost.value, {
    onProjectSelect: (projectId) => emit('select', projectId),
  })
  adapter.setProjects(props.projects)
  adapter.setSelectedProject(props.selectedProjectId)
}

async function mountOffline(version: number, reason: string) {
  if (version !== initializationVersion) {
    return
  }
  const offlineAdapter = new JingweiOfflineMapAdapter()
  await mountAdapter(offlineAdapter)
  if (version !== initializationVersion) {
    offlineAdapter.destroy()
    return
  }
  mapAdapter = offlineAdapter
  emit('providerState', {
    mode: 'OFFLINE',
    label: '经纬离线底座',
    degraded: true,
    reason,
  })
}

async function initializeMap() {
  const version = ++initializationVersion
  mapAdapter?.destroy()
  mapAdapter = null

  try {
    const config = await getMapRuntimeConfig()
    if (version !== initializationVersion) {
      return
    }
    if (config.onlineEnabled) {
      const onlineAdapter = new AmapMapAdapter(config)
      try {
        await mountAdapter(onlineAdapter)
        if (version !== initializationVersion) {
          onlineAdapter.destroy()
          return
        }
        mapAdapter = onlineAdapter
        emit('providerState', {
          mode: 'AMAP',
          label: config.viewMode === '3D' ? '高德 3D 地图' : '高德 2D 地图',
          degraded: false,
          reason: null,
        })
        return
      } catch (error) {
        onlineAdapter.destroy()
        const reason = error instanceof Error ? error.message : '高德地图加载失败'
        await mountOffline(version, reason)
        return
      }
    }
    await mountOffline(version, config.unavailableReason || '在线地图当前不可用')
  } catch {
    await mountOffline(version, '无法读取在线地图配置，已切换本地底座')
  }
}

onMounted(() => {
  void initializeMap()
})

watch(
  () => props.projects,
  (projects) => mapAdapter?.setProjects(projects),
)

watch(
  () => props.selectedProjectId,
  (projectId) => mapAdapter?.setSelectedProject(projectId),
)

onBeforeUnmount(() => {
  initializationVersion += 1
  mapAdapter?.destroy()
  mapAdapter = null
})

defineExpose({
  fitProjects: () => mapAdapter?.fitProjects(),
  focusProject: (projectId: string) => mapAdapter?.focusProject(projectId),
  retry: initializeMap,
})
</script>

<template>
  <div ref="mapHost" class="overview-map-host" aria-label="经纬空间项目总览地图" />
</template>

<style scoped>
.overview-map-host,
.overview-map-host :deep(.jw-map-surface),
.overview-map-host :deep(.jw-amap-map-shell),
.overview-map-host :deep(.jw-amap-surface),
.overview-map-host :deep(.jw-map-canvas),
.overview-map-host :deep(.jw-map-marker-layer) {
  position: absolute;
  inset: 0;
}

.overview-map-host {
  min-width: 0;
  overflow: hidden;
  background: #dfe8e5;
}

.overview-map-host :deep(.jw-amap-map-shell) {
  overflow: hidden;
  background: #d8e5e1;
}

.overview-map-host :deep(.jw-map-brand-layer) {
  position: absolute;
  z-index: 1;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  background: transparent;
  box-shadow: inset 0 0 0 1px rgb(28 93 87 / 10%);
}

.overview-map-host :deep(.jw-map-brand-layer::before),
.overview-map-host :deep(.jw-map-brand-layer::after) {
  position: absolute;
  width: 30px;
  height: 30px;
  border-color: rgb(24 105 97 / 38%);
  content: '';
}

.overview-map-host :deep(.jw-map-brand-layer::before) {
  top: 92px;
  left: 24px;
  border-top: 2px solid;
  border-left: 2px solid;
}

.overview-map-host :deep(.jw-map-brand-layer::after) {
  right: 24px;
  bottom: 54px;
  border-right: 2px solid;
  border-bottom: 2px solid;
}

.overview-map-host :deep(.jw-map-project-marker) {
  position: absolute;
  z-index: 2;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #173f47;
  transform: translate(-50%, -50%);
}

.overview-map-host :deep(.jw-map-project-marker::before) {
  position: absolute;
  inset: -7px;
  border: 1px dashed rgb(38 125 118 / 42%);
  border-radius: 50%;
  content: '';
  opacity: 0.72;
  animation: jw-project-orbit 7s linear infinite;
}

.overview-map-host :deep(.jw-amap-project-marker) {
  position: relative;
  transform: none;
}

.overview-map-host :deep(.jw-map-project-dot) {
  position: absolute;
  inset: 3px;
  z-index: 2;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #267d76;
  box-shadow: 0 3px 10px rgb(18 72 70 / 30%);
}

.overview-map-host :deep(.jw-map-project-pulse) {
  position: absolute;
  inset: -4px;
  border: 1px solid rgb(38 125 118 / 58%);
  border-radius: 50%;
  background: rgb(38 125 118 / 14%);
  animation: jw-project-pulse 2.4s ease-out infinite;
}

.overview-map-host :deep(.jw-map-project-label) {
  position: absolute;
  top: 25px;
  left: 10px;
  display: grid;
  min-width: 142px;
  max-width: 210px;
  gap: 1px;
  padding: 7px 9px;
  border: 1px solid rgb(102 136 130 / 34%);
  border-radius: 5px;
  background: rgb(248 251 250 / 94%);
  box-shadow: 0 7px 18px rgb(34 70 68 / 13%);
  opacity: 0;
  pointer-events: none;
  text-align: left;
  transform: translateY(4px);
  transition:
    opacity 140ms ease,
    transform 140ms ease,
    visibility 140ms ease;
  visibility: hidden;
}

.overview-map-host :deep(.jw-map-project-label strong),
.overview-map-host :deep(.jw-map-project-label small) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-map-host :deep(.jw-map-project-label strong) {
  font-size: 12px;
}

.overview-map-host :deep(.jw-map-project-label small) {
  color: #70838b;
  font-size: 10px;
}

.overview-map-host :deep(.jw-map-project-marker:hover .jw-map-project-label),
.overview-map-host :deep(.jw-map-project-marker:focus-visible .jw-map-project-label) {
  border-color: #267d76;
  background: #fff;
  opacity: 1;
  transform: translateY(0);
  visibility: visible;
}

.overview-map-host :deep(.jw-map-project-marker.is-selected .jw-map-project-dot) {
  background: #b66a35;
  box-shadow:
    0 0 0 5px rgb(182 106 53 / 16%),
    0 4px 12px rgb(93 54 27 / 28%);
}

.overview-map-host :deep(.jw-map-project-marker.is-archived .jw-map-project-dot) {
  background: #77878c;
}

.overview-map-host :deep(.jw-map-project-marker:focus-visible) {
  outline: 2px solid #17645f;
  outline-offset: 5px;
}

.overview-map-host :deep(.jw-map-project-marker.is-label-left .jw-map-project-label) {
  right: 10px;
  left: auto;
}

.overview-map-host :deep(.jw-map-project-marker.is-label-above .jw-map-project-label) {
  top: auto;
  bottom: 25px;
}

@keyframes jw-project-orbit {
  to {
    transform: rotate(360deg);
  }
}

@keyframes jw-project-pulse {
  0% {
    opacity: 0.85;
    transform: scale(0.7);
  }

  75%,
  100% {
    opacity: 0;
    transform: scale(1.75);
  }
}

@media (prefers-reduced-motion: reduce) {
  .overview-map-host :deep(.jw-map-project-marker::before),
  .overview-map-host :deep(.jw-map-project-pulse) {
    animation: none;
  }
}

@media (max-width: 820px) {
  .overview-map-host {
    position: relative;
    inset: auto;
    min-height: 520px;
  }
}
</style>
