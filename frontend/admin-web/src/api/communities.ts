import request from '@/utils/request'
import type { AdminCommunity, CommunityUpsertPayload } from '@/types/community'

export function getCommunities() {
  return request.get<AdminCommunity[]>('/admin/communities')
}

export function createCommunity(payload: CommunityUpsertPayload) {
  return request.post<CommunityUpsertPayload, AdminCommunity>('/admin/communities', payload)
}

export function updateCommunity(communityId: number, payload: CommunityUpsertPayload) {
  return request.put<CommunityUpsertPayload, AdminCommunity>(`/admin/communities/${communityId}`, payload)
}

export function disableCommunity(communityId: number) {
  return request.delete<AdminCommunity>(`/admin/communities/${communityId}`)
}
