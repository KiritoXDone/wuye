import request from '@/utils/request'
import type { RoomType, RoomTypeUpsertPayload } from '@/types/room-type'

export function getRoomTypes(communityId: number) {
  return request.get<RoomType[]>('/admin/room-types', { params: { communityId } })
}

export function createRoomType(payload: RoomTypeUpsertPayload) {
  return request.post<RoomTypeUpsertPayload, RoomType>('/admin/room-types', payload)
}

export function updateRoomType(roomTypeId: number, payload: RoomTypeUpsertPayload) {
  return request.put<RoomTypeUpsertPayload, RoomType>(`/admin/room-types/${roomTypeId}`, payload)
}

export function deleteRoomType(roomTypeId: number) {
  return request.delete<void>(`/admin/room-types/${roomTypeId}`)
}
