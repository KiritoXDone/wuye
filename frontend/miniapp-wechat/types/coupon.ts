export interface CouponValidatePayload {
  billId: number
  couponInstanceId: number
}

export interface CouponValidateResponse {
  couponInstanceId: number
  valid: boolean
  discountAmount: number
  message?: string
}
