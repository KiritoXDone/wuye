export interface AdminRoom {
  id: number
  communityId: number
  buildingNo: string
  unitNo: string
  roomNo: string
  roomTypeId?: number | null
  roomTypeName?: string | null
  areaM2: number | string
  status: number
}

export interface AdminRoomUpdatePayload {
  roomTypeId?: number | null
  areaM2: number
}
