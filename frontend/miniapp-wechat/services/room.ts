import type { RoomItem } from '../types/room'
import { request } from '../utils/request'

export function getMyRooms() {
  return request<RoomItem[]>({
    url: '/api/v1/me/rooms'
  })
}
