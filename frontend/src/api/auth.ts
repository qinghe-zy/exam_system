import http from './http'

export interface CurrentUser {
  id: number
  username: string
  nickname: string
  roleCode: string
}

export interface LoginPayload {
  token: string
  currentUser: CurrentUser
}

export function login(username: string, password: string) {
  return http.post<never, LoginPayload>('/api/auth/login', { username, password })
}

export function fetchCurrentUser() {
  return http.get<never, CurrentUser>('/api/auth/me')
}

export function logout() {
  return http.post('/api/auth/logout')
}
