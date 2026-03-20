import request from '@/utils/request'
import type {
  AdminUser,
  AdminUserCreatePayload,
  AdminUserListQuery,
  AdminUserPasswordResetPayload,
  AdminUserStatusPayload,
} from '@/types/user'

export function getAdminUsers(params?: AdminUserListQuery) {
  return request.get<AdminUser[]>('/admin/accounts', { params })
}

export function createAdminUser(payload: AdminUserCreatePayload) {
  return request.post<AdminUserCreatePayload, AdminUser>('/admin/accounts/admins', payload)
}

export function updateAdminUserStatus(userId: number, payload: AdminUserStatusPayload) {
  return request.put<AdminUserStatusPayload, void>(`/admin/accounts/${userId}/status`, payload)
}

export function resetAdminUserPassword(userId: number, payload: AdminUserPasswordResetPayload) {
  return request.post<AdminUserPasswordResetPayload, void>(`/admin/accounts/${userId}/reset-password`, payload)
}
