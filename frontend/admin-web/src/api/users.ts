import request from '@/utils/request'
import type {
  AdminUser,
  AdminUserCreatePayload,
  AdminUserListQuery,
  AdminUserPasswordResetPayload,
  AdminUserRoom,
  AdminUserStatusUpdatePayload,
} from '@/types/user'

export function getAdminUsers(params?: AdminUserListQuery) {
  return request.get<AdminUser[]>('/admin/accounts', { params })
}

export function createAdminUser(payload: AdminUserCreatePayload) {
  return request.post<AdminUserCreatePayload, AdminUser>('/admin/accounts/admins', payload)
}

export function updateAdminUserStatus(userId: number, payload: AdminUserStatusUpdatePayload) {
  return request.put<AdminUserStatusUpdatePayload, void>(`/admin/accounts/${userId}/status`, payload)
}

export function deleteAdminUser(userId: number) {
  return request.delete<void>(`/admin/accounts/${userId}`)
}

export function resetAdminUserPassword(userId: number, payload: AdminUserPasswordResetPayload) {
  return request.post<AdminUserPasswordResetPayload, void>(`/admin/accounts/${userId}/reset-password`, payload)
}

export function getAdminUserRooms(userId: number) {
  return request.get<AdminUserRoom[]>(`/admin/accounts/${userId}/rooms`)
}

export function unbindAdminUserRoom(userId: number, roomId: number) {
  return request.post(`/admin/accounts/${userId}/rooms/${roomId}/unbind`)
}
