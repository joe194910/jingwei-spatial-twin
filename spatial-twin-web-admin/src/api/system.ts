import { request } from '@/api/http'
import type {
  MenuItem,
  MenuSaveRequest,
  RoleItem,
  RoleSaveRequest,
  SettingItem,
  UserItem,
  UserSaveRequest,
} from '@/types/system'

export function listUsers(keyword = '') {
  const query = keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
  return request<UserItem[]>(`/system/users${query}`)
}

export function createUser(data: UserSaveRequest) {
  return request<number>('/system/users', { method: 'POST', body: data })
}

export function updateUser(userId: number, data: UserSaveRequest) {
  return request<void>(`/system/users/${userId}`, { method: 'PUT', body: data })
}

export function resetUserPassword(userId: number, password: string) {
  return request<void>(`/system/users/${userId}/reset-password`, {
    method: 'POST',
    body: { password },
  })
}

export function deleteUser(userId: number) {
  return request<void>(`/system/users/${userId}`, { method: 'DELETE' })
}

export function listRoles() {
  return request<RoleItem[]>('/system/roles')
}

export function createRole(data: RoleSaveRequest) {
  return request<number>('/system/roles', { method: 'POST', body: data })
}

export function updateRole(roleId: number, data: RoleSaveRequest) {
  return request<void>(`/system/roles/${roleId}`, { method: 'PUT', body: data })
}

export function deleteRole(roleId: number) {
  return request<void>(`/system/roles/${roleId}`, { method: 'DELETE' })
}

export function listMenus() {
  return request<MenuItem[]>('/system/menus')
}

export function createMenu(data: MenuSaveRequest) {
  return request<number>('/system/menus', { method: 'POST', body: data })
}

export function updateMenu(menuId: number, data: MenuSaveRequest) {
  return request<void>(`/system/menus/${menuId}`, { method: 'PUT', body: data })
}

export function deleteMenu(menuId: number) {
  return request<void>(`/system/menus/${menuId}`, { method: 'DELETE' })
}

export function listSettings() {
  return request<SettingItem[]>('/system/settings')
}

export function updateSetting(key: string, value: string) {
  return request<void>(`/system/settings?key=${encodeURIComponent(key)}`, {
    method: 'PUT',
    body: { value },
  })
}
