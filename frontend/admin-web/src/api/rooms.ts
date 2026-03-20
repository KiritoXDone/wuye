import request from '@/utils/request'
import type {
  AdminRoom,
  AdminRoomBatchCreatePayload,
  AdminRoomBatchDeletePayload,
  AdminRoomBatchUpdatePayload,
  AdminRoomCreatePayload,
  AdminRoomListQuery,
  AdminRoomUpdatePayload,
  BatchOperationResult,
} from '@/types/room'

export function getAdminRooms(params: AdminRoomListQuery) {
  return request.get<AdminRoom[]>('/admin/rooms', { params })
}

export function createAdminRoom(payload: AdminRoomCreatePayload) {
  return request.post<AdminRoomCreatePayload, AdminRoom>('/admin/rooms', payload)
}

export function updateAdminRoom(roomId: number, payload: AdminRoomUpdatePayload) {
  return request.put<AdminRoomUpdatePayload, AdminRoom>(`/admin/rooms/${roomId}`, payload)
}

export function disableAdminRoom(roomId: number) {
  return request.delete<AdminRoom>(`/admin/rooms/${roomId}`)
}

export function enableAdminRoom(roomId: number) {
  return request.post<undefined, AdminRoom>(`/admin/rooms/${roomId}/enable`)
}

export function batchCreateAdminRooms(payload: AdminRoomBatchCreatePayload) {
  return request.post<AdminRoomBatchCreatePayload, BatchOperationResult>('/admin/rooms/batch-create', payload)
}

export function batchUpdateAdminRooms(payload: AdminRoomBatchUpdatePayload) {
  return request.post<AdminRoomBatchUpdatePayload, BatchOperationResult>('/admin/rooms/batch-update', payload)
}

export function batchDeleteAdminRooms(payload: AdminRoomBatchDeletePayload) {
  return request.post<AdminRoomBatchDeletePayload, BatchOperationResult>('/admin/rooms/batch-delete', payload)
}
