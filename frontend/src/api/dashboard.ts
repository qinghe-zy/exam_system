import http from './http'

export interface DashboardMetric {
  label: string
  value: number
  description: string
}

export interface DashboardOverview {
  headline: string
  summary: string
  metrics: DashboardMetric[]
  nextActions: string[]
}

export function fetchDashboardOverview() {
  return http.get<never, DashboardOverview>('/api/dashboard/overview')
}
