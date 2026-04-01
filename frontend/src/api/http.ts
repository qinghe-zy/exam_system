import axios from 'axios'
import { ElMessage } from 'element-plus'

import { clearToken, getToken } from '../utils/storage'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  ((response: any) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload.code !== 0) {
      ElMessage.error(payload.message || 'Request failed')
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return payload.data
  }) as any,
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
    }
    ElMessage.error(error.response?.data?.message || error.message || 'Request failed')
    return Promise.reject(error)
  }
)

export default http
