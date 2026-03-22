export interface RoomType {
  id: number
  communityId: number
  typeCode: string
  typeName: string
  areaM2: number | string
}

export interface RoomTypeUpsertPayload {
  communityId: number
  typeCode: string
  typeName: string
  areaM2: number
}
