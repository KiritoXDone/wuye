export interface AdminCommunity {
  id: number
  communityCode: string
  name: string
  status: number
  roomTypeCount: number
  roomCount: number
}

export interface CommunityUpsertPayload {
  communityCode: string
  name: string
  status?: number
}
