import axios from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import type { ApiResponse } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const service = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

service.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body.code !== '0') {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || 'Request failed'))
    }
    return body.data
  },
  async (error) => {
    const authStore = useAuthStore()
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '请求失败'

    if (status === 401) {
      authStore.clearSession()
      if (router.currentRoute.value.path !== '/login') {
        await router.push({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath },
        })
      }
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default service
