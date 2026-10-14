import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import { clearSessionToken, hasSessionToken, saveSessionToken } from '@/api/http'
import type { CurrentUser, MenuItem } from '@/types/system'

interface AuthState {
  user: CurrentUser | null
  menus: MenuItem[]
  initialized: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    menus: [],
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
    async refreshMenus() {
      if (!hasSessionToken()) {
        this.menus = []
        return
      }
      this.menus = await authApi.getCurrentMenus()
    },
    async login(username: string, password: string) {
      const result = await authApi.login(username.trim(), password)
      saveSessionToken(result.tokenName, result.tokenValue)
      try {
        this.user = {
          userId: result.userId,
          username: result.username,
          nickname: result.nickname,
          roles: result.roles,
          permissions: result.permissions,
        }
        await this.refreshMenus()
        this.initialized = true
      } catch (error) {
        clearSessionToken()
        this.user = null
        this.menus = []
        throw error
      }
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
        const [user, menus] = await Promise.all([
          authApi.getCurrentUser(),
          authApi.getCurrentMenus(),
        ])
        this.user = user
        this.menus = menus
      } catch {
        clearSessionToken()
        this.user = null
        this.menus = []
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
        this.menus = []
        this.initialized = true
      }
    },
  },
})
