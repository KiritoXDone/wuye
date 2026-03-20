import request from '@/utils/request'
import type { AdminRoom, AdminRoomUpdatePayload } from '@/types/room'

export function getAdminRooms(communityId: number) {
  return request.get<AdminRoom[]>('/admin/rooms', { params: { communityId } })
}

export function updateAdminRoom(roomId: number, payload: AdminRoomUpdatePayload) {
  return request.put<AdminRoomUpdatePayload, AdminRoom>(`/admin/rooms/${roomId}`, payload)
}
