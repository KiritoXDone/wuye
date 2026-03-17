import type { PaymentVoucher } from '../types/voucher'
import { request } from '../utils/request'

export function getPaymentVoucher(payOrderNo: string) {
  return request<PaymentVoucher>({
    url: `/api/v1/payments/${payOrderNo}/voucher`
  })
}
