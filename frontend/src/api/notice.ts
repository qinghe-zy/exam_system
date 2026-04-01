import http from './http'

export interface Notice {
  id: number
  title: string
  category: string
  status: number
  content: string
  publishTime: string
  updateTime: string
}

export interface NoticePage {
  records: Notice[]
  total: number
  pageNum: number
  pageSize: number
}

export interface NoticePayload {
  title: string
  category: string
  status: number
  content: string
}

export interface NoticeQuery {
  pageNum: number
  pageSize: number
  title?: string
  status?: number | ''
}

export function fetchNoticePage(query: NoticeQuery) {
  return http.get<never, NoticePage>('/api/notices', { params: query })
}

export function fetchNotice(id: number) {
  return http.get<never, Notice>(`/api/notices/${id}`)
}

export function createNotice(payload: NoticePayload) {
  return http.post<never, Notice>('/api/notices', payload)
}

export function updateNotice(id: number, payload: NoticePayload) {
  return http.put<never, Notice>(`/api/notices/${id}`, payload)
}

export function deleteNotice(id: number) {
  return http.delete(`/api/notices/${id}`)
}
