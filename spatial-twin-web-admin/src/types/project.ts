export type ProjectStatus = 'ACTIVE' | 'ARCHIVED'
export type ProjectMapMode = '2D' | '3D'

export interface ProjectStartup {
  mapProvider: string
  mapMode: ProjectMapMode
  centerLng: number
  centerLat: number
  zoomLevel: number
  pitch: number
  rotation: number
}

export interface ProjectItem {
  id: string
  projectCode: string
  name: string
  description: string | null
  locationName: string
  status: ProjectStatus
  currentMemberRole: string | null
  editable: boolean
  archivable: boolean
  deletable: boolean
  startup: ProjectStartup
  createdAt: string
  updatedAt: string
}

export interface ProjectStartupSaveRequest {
  centerLng: number
  centerLat: number
  zoomLevel: number
  pitch: number
  rotation: number
  mapMode: ProjectMapMode
}

export interface ProjectSaveRequest {
  projectCode: string
  name: string
  description: string
  locationName: string
  startup: ProjectStartupSaveRequest
}

export interface ProjectQuery {
  pageNo: number
  pageSize: number
  keyword?: string
  status?: ProjectStatus
}
