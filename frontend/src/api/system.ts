import http from './http'

export interface SystemUser {
  id: number
  username: string
  nickname: string
  fullName?: string
  roleCode: string
  organizationId?: number
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

export interface AuditLogRecord {
  id: number
  operatorName: string
  moduleName: string
  actionName: string
  targetType: string
  targetId?: number
  detailText?: string
  createTime: string
}

export interface OrganizationNode {
  id: number
  orgCode: string
  orgName: string
  orgType: string
  parentId: number
  status: number
  children: OrganizationNode[]
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

export function createUser(payload: {
  username: string
  nickname: string
  fullName: string
  roleCode: string
  organizationId: number
  departmentName?: string
  email?: string
  phone?: string
  candidateNo?: string
  password?: string
  status: number
}) {
  return http.post<never, SystemUser>('/api/system/users', payload)
}

export function updateUser(id: number, payload: {
  username: string
  nickname: string
  fullName: string
  roleCode: string
  organizationId: number
  departmentName?: string
  email?: string
  phone?: string
  candidateNo?: string
  password?: string
  status: number
}) {
  return http.put<never, SystemUser>(`/api/system/users/${id}`, payload)
}

export function importCandidates(payload: {
  items: Array<{
    username: string
    fullName: string
    candidateNo: string
    organizationId: number
    departmentName?: string
    email?: string
    phone?: string
  }>
}) {
  return http.post('/api/system/users/import-candidates', payload)
}

export function fetchRoles() {
  return http.get<never, SystemRole[]>('/api/system/roles')
}

export function fetchOrganizations() {
  return http.get<never, OrganizationNode[]>('/api/system/organizations')
}

export function createOrganization(payload: {
  orgCode: string
  orgName: string
  orgType: string
  parentId: number
  status: number
}) {
  return http.post<never, OrganizationNode>('/api/system/organizations', payload)
}

export function updateOrganization(id: number, payload: {
  orgCode: string
  orgName: string
  orgType: string
  parentId: number
  status: number
}) {
  return http.put<never, OrganizationNode>(`/api/system/organizations/${id}`, payload)
}

export function deleteOrganization(id: number) {
  return http.delete(`/api/system/organizations/${id}`)
}

export function fetchMenus() {
  return http.get<never, SystemMenu[]>('/api/system/menus')
}

export function fetchCurrentMenus() {
  return http.get<never, SystemMenu[]>('/api/system/menus/current')
}

export function fetchAuditLogs() {
  return http.get<never, AuditLogRecord[]>('/api/system/audit-logs')
}
