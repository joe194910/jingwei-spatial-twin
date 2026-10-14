/**
 * 地图坐标统一使用经纬度表达，具体供应商坐标转换由适配器内部处理。
 */
export interface MapCoordinate {
  lng: number
  lat: number
}

/**
 * 空间总览只传递项目轻量信息，不包含私有模型地址和工程详情。
 */
export interface OverviewProjectMarker {
  id: string
  name: string
  locationName: string
  status: 'ACTIVE' | 'ARCHIVED'
  coordinate: MapCoordinate
  viewpoint: {
    mapMode: '2D' | '3D'
    zoom: number
    pitch: number
    rotation: number
  }
}

export interface MapAdapterMountOptions {
  onProjectSelect: (projectId: string) => void
}

export interface MapProviderState {
  mode: 'AMAP' | 'OFFLINE'
  label: string
  degraded: boolean
  reason: string | null
}

export interface MapProviderAdminConfig {
  providerCode: 'AMAP'
  enabled: boolean
  apiVersion: '2.0'
  webKeyConfigured: boolean
  securityJsCodeConfigured: boolean
  mapStyle: string | null
  viewMode: '2D' | '3D'
  defaultPitch: number
  defaultRotation: number
  minZoom: number
  maxZoom: number
  showScale: boolean
  showToolbar: boolean
  showControlBar: boolean
  encryptionReady: boolean
  updatedAt: string
}

export interface MapProviderConfigUpdateRequest {
  enabled: boolean
  apiVersion: '2.0'
  webKey: string
  clearWebKey: boolean
  securityJsCode: string
  clearSecurityJsCode: boolean
  mapStyle: string
  viewMode: '2D' | '3D'
  defaultPitch: number
  defaultRotation: number
  minZoom: number
  maxZoom: number
  showScale: boolean
  showToolbar: boolean
  showControlBar: boolean
}

export interface MapRuntimeConfig {
  onlineEnabled: boolean
  providerCode: 'AMAP'
  apiVersion: '2.0'
  webKey?: string
  securityJsCode?: string
  mapStyle: string | null
  viewMode: '2D' | '3D'
  defaultPitch: number
  defaultRotation: number
  minZoom: number
  maxZoom: number
  showScale: boolean
  showToolbar: boolean
  showControlBar: boolean
  fallbackMode: 'JINGWEI_OFFLINE'
  unavailableReason: string | null
}

/**
 * 地图供应商统一边界。Vue页面只依赖该契约，不直接访问高德或其他地图SDK。
 */
export interface MapAdapter {
  mount(container: HTMLElement, options: MapAdapterMountOptions): Promise<void>
  setProjects(projects: OverviewProjectMarker[]): void
  setSelectedProject(projectId: string | null): void
  focusProject(projectId: string): void
  fitProjects(): void
  resize(): void
  destroy(): void
}
