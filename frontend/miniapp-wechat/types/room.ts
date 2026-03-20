export interface RoomItem {
  roomId: number
  communityId: number
  communityName?: string
  buildingNo?: string
  unitNo?: string
  roomNo?: string
  roomLabel: string
  areaM2: number
  bindingStatus: string
}

export interface RoomBindPayload {
  roomId: number
}

export interface CommunityOption {
  communityId: number
  communityName: string
}

export interface BuildingOption {
  buildingNo: string
}

export interface UnitOption {
  unitNo: string
}

export interface RoomOption {
  roomId: number
  roomNo: string
  roomLabel: string
  areaM2: number
}
