interface AMapMapInstance {
  addControl(control: unknown): void
  setFitView(
    overlays?: AMapMarkerInstance[],
    immediately?: boolean,
    avoid?: number[],
    maxZoom?: number,
  ): void
  setZoomAndCenter(zoom: number, center: [number, number]): void
  setPitch(pitch: number): void
  setRotation(rotation: number): void
  resize(): void
  destroy(): void
}

interface AMapMarkerInstance {
  on(eventName: 'click', handler: () => void): void
  setMap(map: AMapMapInstance | null): void
}

export interface AMapNamespace {
  Map: new (container: HTMLElement, options: Record<string, unknown>) => AMapMapInstance
  Marker: new (options: Record<string, unknown>) => AMapMarkerInstance
  Pixel: new (x: number, y: number) => unknown
  Scale?: new (options?: Record<string, unknown>) => unknown
  ToolBar?: new (options?: Record<string, unknown>) => unknown
  ControlBar?: new (options?: Record<string, unknown>) => unknown
}

declare global {
  interface Window {
    AMap?: AMapNamespace
    _AMapSecurityConfig?: {
      securityJsCode: string
    }
  }
}

const SCRIPT_ID = 'jingwei-amap-js-api'
let loadingPromise: Promise<AMapNamespace> | null = null

/**
 * 按运行时数据库配置加载高德JS API。加载失败后清理脚本，允许网络恢复后重试。
 */
export function loadAmap(
  apiVersion: string,
  webKey: string,
  securityJsCode: string,
): Promise<AMapNamespace> {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (loadingPromise) {
    return loadingPromise
  }

  window._AMapSecurityConfig = { securityJsCode }
  loadingPromise = new Promise<AMapNamespace>((resolve, reject) => {
    const script = document.createElement('script')
    script.id = SCRIPT_ID
    script.async = true
    script.src =
      `https://webapi.amap.com/maps?v=${encodeURIComponent(apiVersion)}` +
      `&key=${encodeURIComponent(webKey)}` +
      '&plugin=AMap.Scale,AMap.ToolBar,AMap.ControlBar'

    const timeoutId = window.setTimeout(() => {
      script.remove()
      loadingPromise = null
      reject(new Error('高德地图加载超时'))
    }, 12_000)

    script.onload = () => {
      window.clearTimeout(timeoutId)
      if (!window.AMap) {
        script.remove()
        loadingPromise = null
        reject(new Error('高德地图SDK未正确初始化'))
        return
      }
      resolve(window.AMap)
    }
    script.onerror = () => {
      window.clearTimeout(timeoutId)
      script.remove()
      loadingPromise = null
      reject(new Error('无法连接高德地图服务'))
    }
    document.head.append(script)
  })

  return loadingPromise
}

export type { AMapMapInstance, AMapMarkerInstance }
