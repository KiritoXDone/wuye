export interface FeeRule {
  id: number
  communityId: number
  feeType: string
  ruleName?: string
  unitPrice: number | string
  cycleType: string
  pricingMode?: string
  effectiveFrom: string
  effectiveTo?: string
  remark?: string
  abnormalAbsThreshold?: number | string
  abnormalMultiplierThreshold?: number | string
  waterTiers?: FeeRuleWaterTier[]
}

export interface FeeRuleWaterTier {
  tierOrder?: number
  startUsage: number | string
  endUsage?: number | string
  unitPrice: number | string
}

export interface FeeRuleCreatePayload {
  communityId: number
  feeType: string
  unitPrice: number
  cycleType: string
  pricingMode?: string
  effectiveFrom: string
  effectiveTo?: string
  remark?: string
  abnormalAbsThreshold?: number
  abnormalMultiplierThreshold?: number
  waterTiers?: FeeRuleWaterTier[]
}
