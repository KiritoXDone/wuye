export interface PaymentVoucher {
  payOrderNo: string
  billId: number
  voucherNo: string
  amount: number
  status: string
  issuedAt?: string
  content?: Record<string, unknown>
}
