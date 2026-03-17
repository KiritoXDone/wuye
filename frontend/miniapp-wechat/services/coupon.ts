import type { CouponValidatePayload, CouponValidateResponse } from '../types/coupon'
import { request } from '../utils/request'

export function validateCoupon(payload: CouponValidatePayload) {
  return request<CouponValidateResponse>({
    url: '/api/v1/coupons/validate',
    method: 'POST',
    data: payload
  })
}
