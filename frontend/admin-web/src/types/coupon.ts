export interface CouponTemplate {
  id: number
  templateCode: string
  type: string
  feeType?: string
  name: string
  discountMode: string
  valueAmount: number | string
  thresholdAmount: number | string
  validFrom: string
  validTo: string
  stackable: number
  status: number
}

export interface CouponTemplateCreatePayload {
  templateCode: string
  type: string
  feeType?: string
  name: string
  discountMode: string
  valueAmount: number
  thresholdAmount: number
  validFrom: string
  validTo: string
}

export interface CouponRule {
  id: number
  ruleName: string
  triggerType: string
  feeType?: string
  templateId: number
  minPayAmount: number | string
  rewardCount: number
  status: number
}

export interface CouponRuleCreatePayload {
  name: string
  triggerType: string
  feeType?: string
  templateCode: string
  minPayAmount: number
  rewardCount: number
}

export interface CouponSummary {
  id: number
  templateCode: string
  type: string
  feeType?: string
  name: string
  goodsName?: string
  goodsSpec?: string
  fulfillmentType?: string
  redeemInstruction?: string
  discountMode: string
  valueAmount: number | string
  thresholdAmount: number | string
  validFrom: string
  validTo: string
  status: number
  ruleId?: number
  ruleName?: string
  triggerType?: string
  minPayAmount?: number | string
  rewardCount?: number
  ruleStatus?: number
  issuedCount: number
}

export interface CouponUpsertPayload {
  id?: number
  templateCode: string
  type: string
  feeType?: string
  name: string
  goodsName?: string
  goodsSpec?: string
  fulfillmentType?: string
  redeemInstruction?: string
  discountMode: string
  valueAmount: number
  thresholdAmount: number
  validFrom: string
  validTo: string
  status?: number
  triggerType?: string
  minPayAmount?: number
  rewardCount?: number
  ruleStatus?: number
}

export interface VoucherExchangeRecord {
  exchangeId: number
  couponInstanceId: number
  templateId: number
  templateName: string
  ownerAccountId: number
  ownerAccountName?: string
  goodsName: string
  goodsSpec?: string
  exchangeStatus: string
  pickupSite?: string
  remark?: string
  createdAt: string
}

export interface VoucherExchangeStatusPayload {
  exchangeStatus: 'FULFILLED' | 'CANCELLED'
  remark?: string
}

export interface CouponInstanceQuery {
  templateId?: number
  templateKeyword?: string
  status?: string
  sourceType?: string
  ownerAccountId?: number
}

export interface CouponInstance {
  couponInstanceId: number
  templateId: number
  templateCode: string
  templateName: string
  templateType: string
  ownerAccountId: number
  ownerAccountName?: string
  sourceType: string
  sourceRefNo?: string
  status: string
  issuedAt: string
  expiresAt: string
}

export interface CouponManualIssuePayload {
  templateId: number
  ownerAccountId: number
  issueCount: number
  remark?: string
}

export interface CouponManualIssueResult {
  templateId: number
  ownerAccountId: number
  issueCount: number
  couponInstanceIds: number[]
}
