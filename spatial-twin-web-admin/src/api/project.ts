import { request } from '@/api/http'
import type { PageResult } from '@/types/api'
import type {
  ProjectItem,
  ProjectQuery,
  ProjectSaveRequest,
  ProjectStartup,
  ProjectStartupSaveRequest,
} from '@/types/project'

export function listProjects(query: ProjectQuery) {
  const params = new URLSearchParams({
    pageNo: String(query.pageNo),
    pageSize: String(query.pageSize),
  })
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return request<PageResult<ProjectItem>>(`/projects?${params.toString()}`)
}

export function getProject(projectId: string) {
  return request<ProjectItem>(`/projects/${encodeURIComponent(projectId)}`)
}

export function createProject(data: ProjectSaveRequest) {
  return request<string>('/projects', { method: 'POST', body: data })
}

export function updateProject(projectId: string, data: ProjectSaveRequest) {
  return request<void>(`/projects/${encodeURIComponent(projectId)}`, {
    method: 'PUT',
    body: data,
  })
}

export function archiveProject(projectId: string) {
  return request<void>(`/projects/${encodeURIComponent(projectId)}/archive`, {
    method: 'POST',
  })
}

export function deleteProject(projectId: string) {
  return request<void>(`/projects/${encodeURIComponent(projectId)}`, {
    method: 'DELETE',
  })
}

export function getProjectStartup(projectId: string) {
  return request<ProjectStartup>(`/projects/${encodeURIComponent(projectId)}/startup`)
}

export function updateProjectStartup(projectId: string, data: ProjectStartupSaveRequest) {
  return request<void>(`/projects/${encodeURIComponent(projectId)}/startup`, {
    method: 'PUT',
    body: data,
  })
}
