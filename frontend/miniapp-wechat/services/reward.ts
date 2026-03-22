import type { RewardVoucherExchangePayload, RewardVoucherItem } from '../types/reward'
import { request } from '../utils/request'

export function getMyRewardVouchers() {
  return request<RewardVoucherItem[]>({
    url: '/api/v1/me/vouchers'
  })
}

export function exchangeRewardVoucher(couponInstanceId: number, payload?: RewardVoucherExchangePayload) {
  return request<RewardVoucherItem>({
    url: `/api/v1/vouchers/${couponInstanceId}/exchange`,
    method: 'POST',
    data: payload || {}
  })
}
