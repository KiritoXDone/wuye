export interface FeeRule {
  id: number
  communityId: number
  feeType: string
  ruleName?: string
  unitPrice: number | string
  cycleType: string
  effectiveFrom: string
  effectiveTo?: string
  remark?: string
}

export interface FeeRuleCreatePayload {
  communityId: number
  feeType: string
  unitPrice: number
  cycleType: string
  effectiveFrom: string
  effectiveTo?: string
  remark?: string
}
