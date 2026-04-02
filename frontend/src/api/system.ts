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

export interface ConfigItemRecord {
  id: number
  configKey: string
  configName: string
  configGroup: string
  configValue: string
  descriptionText?: string
  status: number
}

export interface DictionaryItemRecord {
  id: number
  dictType: string
  itemCode: string
  itemLabel: string
  itemValue?: string
  sortNo: number
  status: number
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

export function fetchAssignableCandidates() {
  return http.get<never, SystemUser[]>('/api/system/users/assignable-candidates')
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

export function fetchConfigItems() {
  return http.get<never, ConfigItemRecord[]>('/api/system/config-center/configs')
}

export function createConfigItem(payload: Omit<ConfigItemRecord, 'id'>) {
  return http.post<never, ConfigItemRecord>('/api/system/config-center/configs', payload)
}

export function updateConfigItem(id: number, payload: Omit<ConfigItemRecord, 'id'>) {
  return http.put<never, ConfigItemRecord>(`/api/system/config-center/configs/${id}`, payload)
}

export function deleteConfigItem(id: number) {
  return http.delete(`/api/system/config-center/configs/${id}`)
}

export function fetchDictionaryItems() {
  return http.get<never, DictionaryItemRecord[]>('/api/system/config-center/dictionaries')
}

export function createDictionaryItem(payload: Omit<DictionaryItemRecord, 'id'>) {
  return http.post<never, DictionaryItemRecord>('/api/system/config-center/dictionaries', payload)
}

export function updateDictionaryItem(id: number, payload: Omit<DictionaryItemRecord, 'id'>) {
  return http.put<never, DictionaryItemRecord>(`/api/system/config-center/dictionaries/${id}`, payload)
}

export function deleteDictionaryItem(id: number) {
  return http.delete(`/api/system/config-center/dictionaries/${id}`)
}
