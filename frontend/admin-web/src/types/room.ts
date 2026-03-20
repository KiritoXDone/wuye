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

export interface AdminRoomListQuery {
  communityId: number
  buildingNo?: string
  unitNo?: string
  roomNoKeyword?: string
  roomSuffix?: string
  roomTypeId?: number
  status?: number
}

export interface AdminRoomCreatePayload {
  communityId: number
  buildingNo: string
  unitNo: string
  roomNo: string
  roomTypeId?: number | null
  areaM2: number
}

export interface AdminRoomUpdatePayload {
  roomTypeId?: number | null
  areaM2?: number
}

export interface AdminRoomBatchCreatePayload {
  communityId: number
  buildingNo: string
  unitNo: string
  roomNos: string[]
  roomTypeId?: number | null
  areaM2: number
}

export interface AdminRoomBatchUpdatePayload extends AdminRoomListQuery {
  selectionRoomIds?: number[]
  applyToFiltered?: boolean
  targetRoomTypeId?: number | null
  targetAreaM2?: number
}

export interface AdminRoomBatchDeletePayload extends AdminRoomListQuery {
  selectionRoomIds?: number[]
  applyToFiltered?: boolean
}

export interface BatchOperationResult {
  requestedCount: number
  successCount: number
  skippedCount: number
  skippedReasons: string[]
}
