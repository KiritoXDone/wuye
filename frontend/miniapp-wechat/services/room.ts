import type { BuildingOption, CommunityOption, RoomBindPayload, RoomItem, RoomOption, UnitOption } from '../types/room'
import { request } from '../utils/request'

export function getMyRooms() {
  return request<RoomItem[]>({
    url: '/api/v1/me/rooms'
  })
}

export function bindMyRoom(data: RoomBindPayload) {
  return request<RoomItem>({
    url: '/api/v1/me/rooms',
    method: 'POST',
    data,
    showLoading: true,
    loadingText: '绑定中'
  })
}

export function unbindMyRoom(roomId: number) {
  return request<void>({
    url: `/api/v1/me/rooms/${roomId}/unbind`,
    method: 'POST',
    showLoading: true,
    loadingText: '解绑中'
  })
}

export function getRoomBindCommunities() {
  return request<CommunityOption[]>({
    url: '/api/v1/me/rooms/options/communities'
  })
}

export function getRoomBindBuildings(communityId: number) {
  return request<BuildingOption[]>({
    url: '/api/v1/me/rooms/options/buildings',
    data: { communityId }
  })
}

export function getRoomBindUnits(communityId: number, buildingNo: string) {
  return request<UnitOption[]>({
    url: '/api/v1/me/rooms/options/units',
    data: { communityId, buildingNo }
  })
}

export function getRoomBindRooms(communityId: number, buildingNo: string, unitNo: string) {
  return request<RoomOption[]>({
    url: '/api/v1/me/rooms/options/room-numbers',
    data: { communityId, buildingNo, unitNo }
  })
}
