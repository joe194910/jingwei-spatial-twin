<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { JingweiMockMapAdapter } from '@/map/adapters/JingweiMockMapAdapter'
import type { MapAdapter, OverviewProjectMarker } from '@/map/types'

interface OverviewMapProps {
  /** 当前用户可见的项目标注集合。 */
  projects: OverviewProjectMarker[]
  /** 用于同步列表与地图高亮状态的项目编号。 */
  selectedProjectId: string | null
}

interface OverviewMapEmits {
  /** 用户从地图标注选择项目时通知上层页面。 */
  select: [projectId: string]
}

const props = defineProps<OverviewMapProps>()
const emit = defineEmits<OverviewMapEmits>()

const mapHost = ref<HTMLDivElement>()
let mapAdapter: MapAdapter | null = null

onMounted(() => {
  if (!mapHost.value) {
    return
  }
  mapAdapter = new JingweiMockMapAdapter()
  mapAdapter.mount(mapHost.value, {
    onProjectSelect: (projectId) => emit('select', projectId),
  })
  mapAdapter.setProjects(props.projects)
  mapAdapter.setSelectedProject(props.selectedProjectId)
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
  mapAdapter?.destroy()
  mapAdapter = null
})

defineExpose({
  fitProjects: () => mapAdapter?.fitProjects(),
  focusProject: (projectId: string) => mapAdapter?.focusProject(projectId),
})
</script>

<template>
  <div ref="mapHost" class="overview-map-host" aria-label="经纬空间项目总览地图" />
</template>

<style scoped>
.overview-map-host,
.overview-map-host :deep(.jw-map-surface),
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
  border: 1px solid rgb(38 125 118 / 52%);
  border-radius: 50%;
  background: rgb(38 125 118 / 10%);
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
  text-align: left;
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
.overview-map-host :deep(.jw-map-project-marker:focus-visible .jw-map-project-label),
.overview-map-host :deep(.jw-map-project-marker.is-selected .jw-map-project-label) {
  border-color: #267d76;
  background: #fff;
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

@media (max-width: 820px) {
  .overview-map-host {
    position: relative;
    inset: auto;
    min-height: 520px;
  }
}
</style>
