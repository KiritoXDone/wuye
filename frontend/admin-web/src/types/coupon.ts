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
  feeType: string
  templateId: number
  minPayAmount: number | string
  rewardCount: number
  status: number
}

export interface CouponRuleCreatePayload {
  name: string
  feeType: string
  templateCode: string
  minPayAmount: number
  rewardCount: number
}
