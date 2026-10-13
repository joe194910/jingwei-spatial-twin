import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import { clearSessionToken, hasSessionToken, saveSessionToken } from '@/api/http'
import type { CurrentUser } from '@/types/system'

interface AuthState {
  user: CurrentUser | null
  initialized: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    initialized: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.user && hasSessionToken()),
    isSuperAdmin: (state) => state.user?.roles.includes('SUPER_ADMIN') ?? false,
  },
  actions: {
    hasPermission(permission: string) {
      return this.isSuperAdmin || Boolean(this.user?.permissions.includes(permission))
    },
    async login(username: string, password: string) {
      const result = await authApi.login(username.trim(), password)
      saveSessionToken(result.tokenName, result.tokenValue)
      this.user = {
        userId: result.userId,
        username: result.username,
        nickname: result.nickname,
        roles: result.roles,
        permissions: result.permissions,
      }
      this.initialized = true
    },
    async restoreSession() {
      if (this.initialized) {
        return
      }
      if (!hasSessionToken()) {
        this.initialized = true
        return
      }
      try {
        this.user = await authApi.getCurrentUser()
      } catch {
        clearSessionToken()
        this.user = null
      } finally {
        this.initialized = true
      }
    },
    async logout() {
      try {
        if (hasSessionToken()) {
          await authApi.logout()
        }
      } finally {
        clearSessionToken()
        this.user = null
        this.initialized = true
      }
    },
  },
})
