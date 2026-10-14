import {
  loadAmap,
  type AMapMapInstance,
  type AMapMarkerInstance,
  type AMapNamespace,
} from '@/map/amap-loader'
import type {
  MapAdapter,
  MapAdapterMountOptions,
  MapRuntimeConfig,
  OverviewProjectMarker,
} from '@/map/types'

interface MarkerEntry {
  marker: AMapMarkerInstance
  element: HTMLButtonElement
  project: OverviewProjectMarker
}

const OVERVIEW_MINIMUM_3D_PITCH = 52
const OVERVIEW_DEFAULT_ROTATION = 342

/**
 * 高德地图适配器。业务组件只传项目数据，不直接依赖高德全局对象。
 */
export class AmapMapAdapter implements MapAdapter {
  private readonly config: MapRuntimeConfig
  private container: HTMLElement | null = null
  private map: AMapMapInstance | null = null
  private projects: OverviewProjectMarker[] = []
  private markers = new Map<string, MarkerEntry>()
  private selectedProjectId: string | null = null
  private onProjectSelect: ((projectId: string) => void) | null = null

  constructor(config: MapRuntimeConfig) {
    this.config = config
  }

  async mount(container: HTMLElement, options: MapAdapterMountOptions) {
    this.destroy()
    if (!this.config.webKey || !this.config.securityJsCode) {
      throw new Error('高德地图凭证不完整')
    }

    const AMap = await loadAmap(
      this.config.apiVersion,
      this.config.webKey,
      this.config.securityJsCode,
    )
    const shell = document.createElement('div')
    shell.className = 'jw-map-surface jw-amap-map-shell'

    const surface = document.createElement('div')
    surface.className = 'jw-amap-surface'

    const brandLayer = document.createElement('div')
    brandLayer.className = 'jw-map-brand-layer'
    brandLayer.setAttribute('aria-hidden', 'true')

    shell.append(surface, brandLayer)
    container.replaceChildren(shell)

    const overviewPitch =
      this.config.viewMode === '3D'
        ? Math.max(this.config.defaultPitch, OVERVIEW_MINIMUM_3D_PITCH)
        : 0
    const overviewRotation =
      this.config.viewMode === '3D' && this.config.defaultRotation === 0
        ? OVERVIEW_DEFAULT_ROTATION
        : this.config.defaultRotation
    const mapOptions: Record<string, unknown> = {
      viewMode: this.config.viewMode,
      pitch: overviewPitch,
      rotation: overviewRotation,
      zoom: 4,
      zooms: [this.config.minZoom, this.config.maxZoom],
      center: [104.195397, 35.86166],
      rotateEnable: this.config.viewMode === '3D',
      pitchEnable: this.config.viewMode === '3D',
      features: ['bg', 'road', 'building'],
      showLabel: true,
      showBuildingBlock: this.config.viewMode === '3D',
      buildingAnimation: this.config.viewMode === '3D',
      mapStyle: this.config.mapStyle || 'amap://styles/normal',
    }

    this.container = container
    this.onProjectSelect = options.onProjectSelect
    this.map = new AMap.Map(surface, mapOptions)
    if (this.config.showScale && AMap.Scale) {
      this.map.addControl(
        new AMap.Scale({ position: { left: '24px', bottom: '62px' } }),
      )
    }
    if (this.config.showToolbar && AMap.ToolBar) {
      this.map.addControl(new AMap.ToolBar({ position: { right: '24px', top: '230px' } }))
    }
    if (this.config.showControlBar && this.config.viewMode === '3D' && AMap.ControlBar) {
      this.map.addControl(new AMap.ControlBar({ position: { right: '24px', top: '92px' } }))
    }
    this.renderMarkers(AMap)
  }

  setProjects(projects: OverviewProjectMarker[]) {
    this.projects = projects
    if (window.AMap && this.map) {
      this.renderMarkers(window.AMap)
      this.fitProjects()
    }
  }

  setSelectedProject(projectId: string | null) {
    this.selectedProjectId = projectId
    this.syncSelectedState()
  }

  focusProject(projectId: string) {
    const entry = this.markers.get(projectId)
    if (!entry || !this.map) {
      return
    }
    this.selectedProjectId = projectId
    this.syncSelectedState()
    this.map.setZoomAndCenter(
      Math.min(this.config.maxZoom, Math.max(this.config.minZoom, entry.project.viewpoint.zoom)),
      [entry.project.coordinate.lng, entry.project.coordinate.lat],
    )
    if (this.config.viewMode === '3D') {
      this.map.setPitch(
        Math.max(entry.project.viewpoint.pitch, OVERVIEW_MINIMUM_3D_PITCH),
      )
      this.map.setRotation(
        entry.project.viewpoint.rotation === 0
          ? OVERVIEW_DEFAULT_ROTATION
          : entry.project.viewpoint.rotation,
      )
    }
  }

  fitProjects() {
    if (!this.map || this.markers.size === 0) {
      return
    }
    this.map.setFitView(
      [...this.markers.values()].map((entry) => entry.marker),
      false,
      [90, 380, 90, 340],
      Math.min(this.config.maxZoom, 16),
    )
    if (this.config.viewMode === '3D') {
      this.map.setPitch(Math.max(this.config.defaultPitch, OVERVIEW_MINIMUM_3D_PITCH))
      this.map.setRotation(
        this.config.defaultRotation === 0
          ? OVERVIEW_DEFAULT_ROTATION
          : this.config.defaultRotation,
      )
    }
  }

  resize() {
    this.map?.resize()
  }

  destroy() {
    for (const entry of this.markers.values()) {
      entry.marker.setMap(null)
    }
    this.markers.clear()
    this.map?.destroy()
    this.map = null
    this.container?.replaceChildren()
    this.container = null
    this.onProjectSelect = null
  }

  private renderMarkers(AMap: AMapNamespace) {
    if (!this.map) {
      return
    }
    for (const entry of this.markers.values()) {
      entry.marker.setMap(null)
    }
    this.markers.clear()

    for (const project of this.projects) {
      const element = this.createMarkerElement(project)
      const marker = new AMap.Marker({
        map: this.map,
        position: [project.coordinate.lng, project.coordinate.lat],
        content: element,
        offset: new AMap.Pixel(-10, -10),
        zIndex: project.id === this.selectedProjectId ? 120 : 100,
      })
      marker.on('click', () => this.onProjectSelect?.(project.id))
      this.markers.set(project.id, { marker, element, project })
    }
    this.syncSelectedState()
  }

  private createMarkerElement(project: OverviewProjectMarker) {
    const marker = document.createElement('button')
    marker.type = 'button'
    marker.className = 'jw-map-project-marker jw-amap-project-marker'
    marker.classList.toggle('is-archived', project.status === 'ARCHIVED')
    marker.setAttribute('aria-label', `查看项目：${project.name}`)

    const pulse = document.createElement('span')
    pulse.className = 'jw-map-project-pulse'
    const dot = document.createElement('span')
    dot.className = 'jw-map-project-dot'
    const label = document.createElement('span')
    label.className = 'jw-map-project-label'
    const name = document.createElement('strong')
    name.textContent = project.name
    const location = document.createElement('small')
    location.textContent = project.locationName
    label.append(name, location)
    marker.append(pulse, dot, label)
    return marker
  }

  private syncSelectedState() {
    for (const [projectId, entry] of this.markers) {
      entry.element.classList.toggle('is-selected', projectId === this.selectedProjectId)
    }
  }
}
