import { API_BASE_URL } from '../config/env'
import type { ApiResponse } from '../types/api'
import { clearAuthSession, getAccessToken } from './auth'

interface RequestOptions {
  url: string
  method?: WechatMiniprogram.RequestOption['method']
  data?: WechatMiniprogram.IAnyObject | string | ArrayBuffer
  withAuth?: boolean
  showLoading?: boolean
  loadingText?: string
}

let redirecting = false

function redirectToLogin(message?: string) {
  clearAuthSession()
  if (redirecting) {
    return
  }

  redirecting = true
  if (message) {
    wx.showToast({ title: message, icon: 'none', duration: 1800 })
  }

  setTimeout(() => {
    wx.reLaunch({ url: '/pages/login/index' })
    redirecting = false
  }, 200)
}

export function request<T>(options: RequestOptions): Promise<T> {
  const {
    url,
    method = 'GET',
    data,
    withAuth = true,
    showLoading = false,
    loadingText = '加载中'
  } = options

  const token = getAccessToken()
  if (withAuth && !token) {
    redirectToLogin('登录状态已失效，请重新登录')
    return Promise.reject(new Error('登录状态已失效'))
  }

  if (showLoading) {
    wx.showLoading({ title: loadingText, mask: true })
  }

  return new Promise((resolve, reject) => {
    wx.request<ApiResponse<T>>({
      url: `${API_BASE_URL}${url}`,
      method,
      timeout: 10000,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(withAuth && token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (response) => {
        const apiResponse = response.data
        const code = apiResponse?.code
        const message = apiResponse?.message || '请求失败'

        if (response.statusCode >= 200 && response.statusCode < 300 && code === '0') {
          resolve(apiResponse.data)
          return
        }

        if (response.statusCode === 401 || code === 'UNAUTHORIZED') {
          redirectToLogin(message)
        }

        reject(new Error(message))
      },
      fail: () => {
        reject(new Error('网络请求失败，请确认后端已启动并关闭了请求域名校验'))
      },
      complete: () => {
        if (showLoading) {
          wx.hideLoading()
        }
      }
    })
  })
}
