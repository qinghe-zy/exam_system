import { defineStore } from 'pinia'

import { fetchCurrentUser, login as loginRequest, logout as logoutRequest, type CurrentUser } from '../api/auth'
import { fetchCurrentMenus, type SystemMenu } from '../api/system'
import { clearToken, getToken, setToken } from '../utils/storage'

interface AuthState {
  token: string
  currentUser: CurrentUser | null
  menus: SystemMenu[]
  initialized: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken() || '',
    currentUser: null,
    menus: [],
    initialized: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    async login(username: string, password: string) {
      const payload = await loginRequest(username, password)
      this.token = payload.token
      this.currentUser = payload.currentUser
      setToken(payload.token)
      await this.loadMenus()
      this.initialized = true
    },
    async bootstrap() {
      if (!this.token) {
        this.initialized = true
        return
      }

      try {
        this.currentUser = await fetchCurrentUser()
        await this.loadMenus()
      } catch {
        this.reset()
      } finally {
        this.initialized = true
      }
    },
    async loadMenus() {
      this.menus = await fetchCurrentMenus()
    },
    async logout() {
      try {
        if (this.token) {
          await logoutRequest()
        }
      } finally {
        this.reset()
      }
    },
    reset() {
      this.token = ''
      this.currentUser = null
      this.menus = []
      this.initialized = true
      clearToken()
    }
  }
})
