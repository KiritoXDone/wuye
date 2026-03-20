import axios, { type AxiosRequestConfig } from 'axios'

import type { ApiResponse } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function unwrapResponse<T>(response: { data: ApiResponse<T> }) {
  const body = response.data
  if (body.code !== '0') {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '请求失败'

    if (status === 401) {
      useAuthStore.getState().clearSession()
      if (window.location.pathname !== '/login') {
        const redirect = `${window.location.pathname}${window.location.search}`
        window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
      }
    }

    return Promise.reject(new Error(message))
  },
)

const request = {
  async get<R = unknown>(url: string, config?: AxiosRequestConfig) {
    return unwrapResponse<R>(await client.get(url, config))
  },
  async post<T = unknown, R = unknown>(url: string, data?: T, config?: AxiosRequestConfig) {
    return unwrapResponse<R>(await client.post(url, data, config))
  },
  async put<T = unknown, R = unknown>(url: string, data?: T, config?: AxiosRequestConfig) {
    return unwrapResponse<R>(await client.put(url, data, config))
  },
  async delete<R = unknown>(url: string, config?: AxiosRequestConfig) {
    return unwrapResponse<R>(await client.delete(url, config))
  },
}

export default request
