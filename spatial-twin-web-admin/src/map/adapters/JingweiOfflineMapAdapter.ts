import type {
  MapAdapter,
  MapAdapterMountOptions,
  OverviewProjectMarker,
} from '@/map/types'

interface ProjectScreenPoint {
  project: OverviewProjectMarker
  x: number
  y: number
}

/**
 * 高德不可用或设备离线时使用的经纬本地底座。
 *
 * 该实现不请求外网，不缓存第三方地图瓦片。背景仅表达空间关系，不代表真实道路和地貌。
 */
export class JingweiOfflineMapAdapter implements MapAdapter {
  private container: HTMLElement | null = null
  private root: HTMLDivElement | null = null
  private canvas: HTMLCanvasElement | null = null
  private markerLayer: HTMLDivElement | null = null
  private projects: OverviewProjectMarker[] = []
  private selectedProjectId: string | null = null
  private onProjectSelect: ((projectId: string) => void) | null = null
  private resizeObserver: ResizeObserver | null = null

  mount(container: HTMLElement, options: MapAdapterMountOptions) {
    this.destroy()
    this.container = container
    this.onProjectSelect = options.onProjectSelect

    const root = document.createElement('div')
    root.className = 'jw-map-surface'

    const canvas = document.createElement('canvas')
    canvas.className = 'jw-map-canvas'
    canvas.setAttribute('aria-hidden', 'true')

    const markerLayer = document.createElement('div')
    markerLayer.className = 'jw-map-marker-layer'

    root.append(canvas, markerLayer)
    container.replaceChildren(root)

    this.root = root
    this.canvas = canvas
    this.markerLayer = markerLayer
    this.resizeObserver = new ResizeObserver(() => this.resize())
    this.resizeObserver.observe(container)
    this.resize()
    return Promise.resolve()
  }

  setProjects(projects: OverviewProjectMarker[]) {
    this.projects = projects
    this.render()
  }

  setSelectedProject(projectId: string | null) {
    this.selectedProjectId = projectId
    this.renderMarkers()
  }

  focusProject(projectId: string) {
    if (!this.projects.some((project) => project.id === projectId)) {
      return
    }
    this.selectedProjectId = projectId
    this.renderMarkers()
  }

  fitProjects() {
    this.render()
  }

  resize() {
    this.render()
  }

  destroy() {
    this.resizeObserver?.disconnect()
    this.resizeObserver = null
    this.container?.replaceChildren()
    this.container = null
    this.root = null
    this.canvas = null
    this.markerLayer = null
    this.onProjectSelect = null
  }

  private render() {
    this.drawBaseMap()
    this.renderMarkers()
  }

  private drawBaseMap() {
    if (!this.canvas || !this.root) {
      return
    }

    const width = Math.max(1, this.root.clientWidth)
    const height = Math.max(1, this.root.clientHeight)
    const pixelRatio = Math.min(window.devicePixelRatio || 1, 2)
    this.canvas.width = Math.floor(width * pixelRatio)
    this.canvas.height = Math.floor(height * pixelRatio)
    this.canvas.style.width = `${width}px`
    this.canvas.style.height = `${height}px`

    const context = this.canvas.getContext('2d')
    if (!context) {
      return
    }

    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
    context.clearRect(0, 0, width, height)
    context.fillStyle = '#dfe8e5'
    context.fillRect(0, 0, width, height)

    this.drawTerrain(context, width, height)
    this.drawGrid(context, width, height)
    this.drawWaterway(context, width, height)
    this.drawRoads(context, width, height)
  }

  private drawTerrain(context: CanvasRenderingContext2D, width: number, height: number) {
    const regions = [
      {
        color: '#cadbd3',
        points: [
          [0.03, 0.16],
          [0.3, 0.05],
          [0.47, 0.22],
          [0.4, 0.48],
          [0.12, 0.57],
        ],
      },
      {
        color: '#d2ded4',
        points: [
          [0.56, 0.04],
          [0.94, 0.12],
          [0.88, 0.45],
          [0.66, 0.56],
          [0.48, 0.31],
        ],
      },
      {
        color: '#c4d5cf',
        points: [
          [0.16, 0.64],
          [0.45, 0.48],
          [0.74, 0.61],
          [0.91, 0.94],
          [0.09, 0.93],
        ],
      },
    ]

    for (const region of regions) {
      context.beginPath()
      region.points.forEach(([x, y], index) => {
        const px = x * width
        const py = y * height
        if (index === 0) context.moveTo(px, py)
        else context.lineTo(px, py)
      })
      context.closePath()
      context.fillStyle = region.color
      context.fill()
    }

    context.strokeStyle = 'rgb(70 112 101 / 20%)'
    context.lineWidth = 1
    for (let index = 0; index < 8; index += 1) {
      context.beginPath()
      context.ellipse(
        width * (0.18 + index * 0.09),
        height * (0.24 + (index % 3) * 0.23),
        width * (0.09 + index * 0.008),
        height * (0.05 + index * 0.004),
        -0.25,
        0,
        Math.PI * 2,
      )
      context.stroke()
    }
  }

  private drawGrid(context: CanvasRenderingContext2D, width: number, height: number) {
    context.strokeStyle = 'rgb(53 91 84 / 10%)'
    context.lineWidth = 1
    const gridSize = 54
    for (let x = gridSize; x < width; x += gridSize) {
      context.beginPath()
      context.moveTo(x, 0)
      context.lineTo(x, height)
      context.stroke()
    }
    for (let y = gridSize; y < height; y += gridSize) {
      context.beginPath()
      context.moveTo(0, y)
      context.lineTo(width, y)
      context.stroke()
    }
  }

  private drawWaterway(context: CanvasRenderingContext2D, width: number, height: number) {
    context.beginPath()
    context.moveTo(width * 0.05, height * 0.73)
    context.bezierCurveTo(
      width * 0.28,
      height * 0.55,
      width * 0.43,
      height * 0.86,
      width * 0.62,
      height * 0.68,
    )
    context.bezierCurveTo(
      width * 0.77,
      height * 0.53,
      width * 0.87,
      height * 0.66,
      width,
      height * 0.52,
    )
    context.strokeStyle = '#96bbc1'
    context.lineWidth = Math.max(12, width * 0.012)
    context.lineCap = 'round'
    context.stroke()
    context.strokeStyle = 'rgb(237 247 246 / 72%)'
    context.lineWidth = 2
    context.stroke()
  }

  private drawRoads(context: CanvasRenderingContext2D, width: number, height: number) {
    const roads = [
      [0.02, 0.3, 0.3, 0.18, 0.66, 0.42, 1.02, 0.26],
      [-0.02, 0.88, 0.28, 0.7, 0.61, 0.85, 1.03, 0.72],
      [0.23, -0.03, 0.3, 0.23, 0.43, 0.52, 0.38, 1.03],
      [0.72, -0.02, 0.64, 0.27, 0.8, 0.47, 0.77, 1.03],
    ]

    context.lineCap = 'round'
    for (const road of roads) {
      context.beginPath()
      context.moveTo(road[0] * width, road[1] * height)
      context.bezierCurveTo(
        road[2] * width,
        road[3] * height,
        road[4] * width,
        road[5] * height,
        road[6] * width,
        road[7] * height,
      )
      context.strokeStyle = 'rgb(255 255 255 / 82%)'
      context.lineWidth = 7
      context.stroke()
      context.strokeStyle = 'rgb(90 120 114 / 34%)'
      context.lineWidth = 1
      context.stroke()
    }
  }

  private renderMarkers() {
    if (!this.markerLayer || !this.root) {
      return
    }
    this.markerLayer.replaceChildren()
    const points = this.projectScreenPoints(this.root.clientWidth, this.root.clientHeight)

    for (const { project, x, y } of points) {
      const marker = document.createElement('button')
      marker.type = 'button'
      marker.className = 'jw-map-project-marker'
      marker.classList.toggle('is-selected', project.id === this.selectedProjectId)
      marker.classList.toggle('is-archived', project.status === 'ARCHIVED')
      marker.classList.toggle('is-label-left', x > this.root.clientWidth * 0.68)
      marker.classList.toggle('is-label-above', y > this.root.clientHeight * 0.72)
      marker.style.left = `${x}px`
      marker.style.top = `${y}px`
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
      marker.addEventListener('click', () => this.onProjectSelect?.(project.id))
      this.markerLayer.append(marker)
    }
  }

  private projectScreenPoints(width: number, height: number): ProjectScreenPoint[] {
    if (this.projects.length === 0) {
      return []
    }

    const longitudes = this.projects.map((project) => project.coordinate.lng)
    const latitudes = this.projects.map((project) => project.coordinate.lat)
    const minLng = Math.min(...longitudes)
    const maxLng = Math.max(...longitudes)
    const minLat = Math.min(...latitudes)
    const maxLat = Math.max(...latitudes)
    const lngRange = Math.max(maxLng - minLng, 0.2)
    const latRange = Math.max(maxLat - minLat, 0.2)
    const horizontalPadding = Math.min(160, Math.max(72, width * 0.13))
    const verticalPadding = Math.min(120, Math.max(70, height * 0.16))
    const usableWidth = Math.max(1, width - horizontalPadding * 2)
    const usableHeight = Math.max(1, height - verticalPadding * 2)
    const lngOffset = (lngRange - (maxLng - minLng)) / 2
    const latOffset = (latRange - (maxLat - minLat)) / 2

    return this.projects.map((project) => ({
      project,
      x:
        horizontalPadding +
        ((project.coordinate.lng - (minLng - lngOffset)) / lngRange) * usableWidth,
      y:
        height -
        verticalPadding -
        ((project.coordinate.lat - (minLat - latOffset)) / latRange) * usableHeight,
    }))
  }
}
