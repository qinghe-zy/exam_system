import http from './http'

export interface SystemUser {
  id: number
  username: string
  nickname: string
  fullName?: string
  roleCode: string
  organizationName?: string
  departmentName?: string
  email?: string
  status: number
}

export interface SystemRole {
  id: number
  roleCode: string
  roleName: string
  remark: string
}

export interface SystemMenu {
  id: number
  name: string
  path: string
  component: string
  icon: string
  permissionCode: string
  parentId: number
  sortNo: number
  menuType: string
  children: SystemMenu[]
}

export function fetchUsers() {
  return http.get<never, SystemUser[]>('/api/system/users')
}

export function fetchRoles() {
  return http.get<never, SystemRole[]>('/api/system/roles')
}

export function fetchMenus() {
  return http.get<never, SystemMenu[]>('/api/system/menus')
}

export function fetchCurrentMenus() {
  return http.get<never, SystemMenu[]>('/api/system/menus/current')
}
