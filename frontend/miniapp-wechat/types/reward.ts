export interface RewardVoucherItem {
  couponInstanceId: number
  templateId: number
  templateCode: string
  name: string
  goodsName?: string
  goodsSpec?: string
  redeemInstruction?: string
  status: string
  exchangeStatus?: string
  pickupSite?: string
  issuedAt: string
  expiresAt: string
}

export interface RewardVoucherExchangePayload {
  remark?: string
}
