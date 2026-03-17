export interface WechatPayParams {
  appId?: string
  timeStamp: string
  nonceStr: string
  package: string
  signType: string
  paySign: string
  tradeNo?: string
  orderString?: string
}

export interface PaymentCreatePayload {
  billId: number
  channel: 'WECHAT' | 'ALIPAY'
  couponInstanceId?: number
  idempotencyKey: string
}

export interface PaymentCreateResponse {
  payOrderNo: string
  originAmount: number
  discountAmount: number
  payAmount: number
  channel: string
  payParams: WechatPayParams
}

export interface PaymentStatusResponse {
  payOrderNo: string
  billId: number
  status: string
  paidAt?: string
  rewardIssuedCount?: number
}
