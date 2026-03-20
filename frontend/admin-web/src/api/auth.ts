import request from '@/utils/request'
import type { LoginPayload, LoginResult, Profile } from '@/types/auth'

export function loginByPassword(payload: LoginPayload) {
  return request.post<LoginPayload, LoginResult>('/admin/auth/login/password', payload)
}

export function getMyProfile() {
  return request.get<Profile>('/me/profile')
}

export function postLogout() {
  return request.post('/auth/logout')
}
