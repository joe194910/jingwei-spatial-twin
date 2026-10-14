import { request } from '@/api/http'
import type {
  MapProviderAdminConfig,
  MapProviderConfigUpdateRequest,
  MapRuntimeConfig,
} from '@/map/types'

export function getMapProviderConfig() {
  return request<MapProviderAdminConfig>('/system/map-provider-config')
}

export function updateMapProviderConfig(data: MapProviderConfigUpdateRequest) {
  return request<void>('/system/map-provider-config', {
    method: 'PUT',
    body: data,
  })
}

export function getMapRuntimeConfig() {
  return request<MapRuntimeConfig>('/map/runtime-config')
}
