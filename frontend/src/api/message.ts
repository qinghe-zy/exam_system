import http from './http'

export interface InAppMessage {
  id: number
  title: string
  messageType: string
  content: string
  relatedType?: string
  relatedId?: number
  readFlag: number
  createTime: string
}

export function fetchMyMessages() {
  return http.get<never, InAppMessage[]>('/api/messages/my')
}

export function markMessageRead(id: number) {
  return http.post(`/api/messages/${id}/read`)
}
