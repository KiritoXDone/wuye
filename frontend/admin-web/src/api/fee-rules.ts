import request from '@/utils/request'
import type { FeeRule, FeeRuleCreatePayload } from '@/types/fee-rule'

export function getFeeRules(communityId: number) {
  return request.get<FeeRule[]>('/admin/fee-rules', {
    params: { communityId },
  })
}

export function createFeeRule(payload: FeeRuleCreatePayload) {
  return request.post<FeeRuleCreatePayload, FeeRule>('/admin/fee-rules', payload)
}
