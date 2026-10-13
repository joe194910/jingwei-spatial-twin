import { request } from '@/api/http'
import type { CurrentUser, LoginResult } from '@/types/system'

export function login(username: string, password: string) {
  return request<LoginResult>('/auth/login', {
    method: 'POST',
    body: { username, password },
  })
}

export function getCurrentUser() {
  return request<CurrentUser>('/auth/me')
}

export function logout() {
  return request<void>('/auth/logout', { method: 'POST' })
}
