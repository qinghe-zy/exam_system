import http from './http'

export interface NotificationTemplate {
  id: number
  organizationId?: number
  templateCode: string
  templateName: string
  businessType: string
  channelType: string
  titleTemplate: string
  contentTemplate: string
  status: number
  updateTime: string
}

export interface NotificationTemplatePayload {
  templateCode: string
  templateName: string
  businessType: string
  channelType: string
  titleTemplate: string
  contentTemplate: string
  status: number
}

export interface NotificationDeliveryLog {
  id: number
  organizationId?: number
  businessType: string
  channelType: string
  templateCode: string
  recipientUserId?: number
  recipientName?: string
  recipientTarget?: string
  title?: string
  content: string
  relatedType?: string
  relatedId?: number
  businessKey: string
  deliveryStatus: string
  providerTrace?: string
  sentAt: string
}

export interface NotificationDeliveryLogPage {
  records: NotificationDeliveryLog[]
  total: number
  pageNum: number
  pageSize: number
}

export interface NotificationDeliveryLogQuery {
  pageNum: number
  pageSize: number
  businessType?: string
  channelType?: string
  deliveryStatus?: string
  recipientUserId?: number | ''
}

export function fetchNotificationTemplates() {
  return http.get<never, NotificationTemplate[]>('/api/notifications/templates')
}

export function createNotificationTemplate(payload: NotificationTemplatePayload) {
  return http.post<never, NotificationTemplate>('/api/notifications/templates', payload)
}

export function updateNotificationTemplate(id: number, payload: NotificationTemplatePayload) {
  return http.put<never, NotificationTemplate>(`/api/notifications/templates/${id}`, payload)
}

export function deleteNotificationTemplate(id: number) {
  return http.delete(`/api/notifications/templates/${id}`)
}

export function fetchNotificationDeliveryLogs(query: NotificationDeliveryLogQuery) {
  return http.get<never, NotificationDeliveryLogPage>('/api/notifications/delivery-logs', { params: query })
}

export function dispatchUpcomingExamReminders() {
  return http.post<never, number>('/api/notifications/exam-reminders/dispatch')
}
