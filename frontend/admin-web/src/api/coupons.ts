import request from '@/utils/request'
import type {
  CouponRule,
  CouponRuleCreatePayload,
  CouponTemplate,
  CouponTemplateCreatePayload,
} from '@/types/coupon'

export function getCouponTemplates() {
  return request.get<CouponTemplate[]>('/admin/coupon-templates')
}

export function createCouponTemplate(payload: CouponTemplateCreatePayload) {
  return request.post<CouponTemplateCreatePayload, CouponTemplate>('/admin/coupon-templates', payload)
}

export function getCouponRules() {
  return request.get<CouponRule[]>('/admin/coupon-rules')
}

export function createCouponRule(payload: CouponRuleCreatePayload) {
  return request.post<CouponRuleCreatePayload, CouponRule>('/admin/coupon-rules', payload)
}
