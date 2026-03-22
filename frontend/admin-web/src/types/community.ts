export interface AdminCommunity {
  id: number
  communityCode: string
  name: string
  roomTypeCount: number
  roomCount: number
}

export interface CommunityUpsertPayload {
  communityCode: string
  name: string
}
