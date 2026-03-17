import type { LoginResponse, WechatLoginPayload } from '../types/auth'
import { request } from '../utils/request'

export function loginWechat(payload: WechatLoginPayload) {
  return request<LoginResponse>({
    url: '/api/v1/auth/login/wechat',
    method: 'POST',
    data: payload,
    withAuth: false,
    showLoading: true,
    loadingText: '登录中'
  })
}
