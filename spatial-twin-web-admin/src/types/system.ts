export interface CurrentUser {
  userId: number
  username: string
  nickname: string
  roles: string[]
  permissions: string[]
}

export interface LoginResult extends CurrentUser {
  tokenName: string
  tokenValue: string
}

export interface UserItem {
  id: number
  username: string
  nickname: string
  status: number
  lockedUntil: string | null
  lastLoginAt: string | null
  roleIds: number[]
  roleNames: string[]
}

export interface UserSaveRequest {
  username: string
  nickname: string
  status: number
  roleIds: number[]
  password?: string
}

export interface RoleItem {
  id: number
  code: string
  name: string
  status: number
  description: string | null
  menuIds: number[]
  userCount: number
}

export interface RoleSaveRequest {
  code: string
  name: string
  status: number
  description: string
  menuIds: number[]
}

export interface MenuItem {
  id: number
  parentId: number
  name: string
  path: string
  component: string | null
  icon: string | null
  sortOrder: number
  permissionCode: string | null
  visible: number
  children: MenuItem[]
}

export interface MenuSaveRequest {
  parentId: number
  name: string
  path: string
  component: string
  icon: string
  sortOrder: number
  permissionCode: string
  visible: number
}

export interface SettingItem {
  key: string
  value: string
  valueType: 'INTEGER' | 'BOOLEAN' | string
  description: string
  sensitive: boolean
}
