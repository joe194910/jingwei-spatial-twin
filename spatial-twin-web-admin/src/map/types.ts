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
}

export interface MapAdapterMountOptions {
  onProjectSelect: (projectId: string) => void
}

/**
 * 地图供应商统一边界。Vue页面只依赖该契约，不直接访问高德或其他地图SDK。
 */
export interface MapAdapter {
  mount(container: HTMLElement, options: MapAdapterMountOptions): void
  setProjects(projects: OverviewProjectMarker[]): void
  setSelectedProject(projectId: string | null): void
  focusProject(projectId: string): void
  fitProjects(): void
  resize(): void
  destroy(): void
}
