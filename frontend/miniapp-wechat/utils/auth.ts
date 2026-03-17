import type { LoginResponse } from '../types/auth'

const AUTH_STORAGE_KEY = 'wuye-miniapp-auth-session'

export function getAuthSession(): LoginResponse | null {
  return wx.getStorageSync(AUTH_STORAGE_KEY) || null
}

export function setAuthSession(session: LoginResponse) {
  wx.setStorageSync(AUTH_STORAGE_KEY, session)
}

export function clearAuthSession() {
  wx.removeStorageSync(AUTH_STORAGE_KEY)
}

export function getAccessToken(): string {
  return getAuthSession()?.accessToken || ''
}

export function hasAuthSession(): boolean {
  return Boolean(getAccessToken())
}
