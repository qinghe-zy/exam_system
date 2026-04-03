import http from './http'

export interface CurrentUser {
  id: number
  username: string
  nickname: string
  fullName?: string
  roleCode: string
  organizationName?: string
  permissions: string[]
}

export interface LoginPayload {
  token: string
  currentUser: CurrentUser
}

export function login(username: string, password: string) {
  return http.post<never, LoginPayload>('/api/auth/login', { username, password })
}

export interface RegisterOption {
  organizationId: number
  organizationName: string
  organizationType: string
}

export interface VerificationCodeSendResult {
  purpose: string
  channel: string
  targetValue: string
  expiresAt: string
  deliveryTrace: string
  mockCode?: string | null
}

export function fetchRegisterOptions() {
  return http.get<never, RegisterOption[]>('/api/auth/register-options')
}

export function sendVerificationCode(payload: {
  purpose: string
  channel: string
  targetValue: string
  username?: string
  organizationId?: number
}) {
  return http.post<never, VerificationCodeSendResult>('/api/auth/verification-codes/send', payload)
}

export function registerAccount(payload: {
  username: string
  fullName: string
  organizationId: number
  departmentName?: string
  email?: string
  phone?: string
  password: string
  verificationCode: string
  verificationChannel: string
}) {
  return http.post('/api/auth/register', payload)
}

export function resetPassword(payload: {
  username: string
  verificationCode: string
  verificationChannel: string
  newPassword: string
}) {
  return http.post('/api/auth/password/reset', payload)
}

export function fetchCurrentUser() {
  return http.get<never, CurrentUser>('/api/auth/me')
}

export function logout() {
  return http.post('/api/auth/logout')
}
