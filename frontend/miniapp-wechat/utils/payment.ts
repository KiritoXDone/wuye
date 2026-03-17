import type { PaymentCreateResponse, WechatPayParams } from '../types/payment'

export function buildPaymentIdempotencyKey(billId: number): string {
  const randomPart = Math.random().toString(36).slice(2, 8)
  return `pay-${billId}-${Date.now()}-${randomPart}`
}

export function canInvokeWechatPayment(payParams?: WechatPayParams): boolean {
  if (!payParams) {
    return false
  }
  return payParams.appId !== 'wx-dev-mock' && payParams.paySign !== 'mock-signature'
}

export function invokeWechatPayment(payment: PaymentCreateResponse): Promise<void> {
    if (!canInvokeWechatPayment(payment.payParams)) {
        return Promise.resolve()
  }

  return new Promise((resolve, reject) => {
    wx.requestPayment({
      timeStamp: payment.payParams.timeStamp,
      nonceStr: payment.payParams.nonceStr,
      package: payment.payParams.package,
      signType: payment.payParams.signType as 'RSA' | 'MD5' | 'HMAC-SHA256',
      paySign: payment.payParams.paySign,
      success: () => resolve(),
      fail: (error) => reject(error)
    })
  })
}

export function invokePaymentByChannel(payment: PaymentCreateResponse): Promise<void> {
  if (payment.channel === 'ALIPAY') {
    return Promise.resolve()
  }
  return invokeWechatPayment(payment)
}
