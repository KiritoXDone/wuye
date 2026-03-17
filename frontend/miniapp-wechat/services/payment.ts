import type { PaymentCreatePayload, PaymentCreateResponse, PaymentStatusResponse } from '../types/payment'
import { request } from '../utils/request'

export function createPayment(payload: PaymentCreatePayload) {
  return request<PaymentCreateResponse>({
    url: '/api/v1/payments',
    method: 'POST',
    data: payload,
    showLoading: true,
    loadingText: '创建支付单'
  })
}

export function getPaymentStatus(payOrderNo: string) {
  return request<PaymentStatusResponse>({
    url: `/api/v1/payments/${payOrderNo}`
  })
}
