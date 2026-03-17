import request from '@/utils/request'
import type {
  CouponRule,
  CouponRuleCreatePayload,
  CouponTemplate,
  CouponTemplateCreatePayload,
} from '@/types/coupon'

export function getCouponTemplates() {
  return request.get<never, CouponTemplate[]>('/admin/coupon-templates')
}

export function createCouponTemplate(payload: CouponTemplateCreatePayload) {
  return request.post<never, CouponTemplate>('/admin/coupon-templates', payload)
}

export function getCouponRules() {
  return request.get<never, CouponRule[]>('/admin/coupon-rules')
}

export function createCouponRule(payload: CouponRuleCreatePayload) {
  return request.post<never, CouponRule>('/admin/coupon-rules', payload)
}
