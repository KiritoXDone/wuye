import request from '@/utils/request'
import type {
  CouponInstance,
  CouponInstanceQuery,
  CouponManualIssuePayload,
  CouponManualIssueResult,
  CouponRule,
  CouponRuleCreatePayload,
  CouponSummary,
  CouponTemplate,
  CouponTemplateCreatePayload,
  CouponUpsertPayload,
  VoucherExchangeRecord,
  VoucherExchangeStatusPayload,
} from '@/types/coupon'

export function getCouponTemplates() {
  return request.get<CouponTemplate[]>('/admin/coupon-templates')
}

export function getCoupons() {
  return request.get<CouponSummary[]>('/admin/coupons')
}

export function saveCoupon(payload: CouponUpsertPayload) {
  return request.post<CouponUpsertPayload, CouponSummary>('/admin/coupons', payload)
}

export function createCouponTemplate(payload: CouponTemplateCreatePayload) {
  return request.post<CouponTemplateCreatePayload, CouponTemplate>('/admin/coupon-templates', payload)
}

export function deleteCouponTemplate(templateId: number) {
  return request.delete(`/admin/coupon-templates/${templateId}`)
}

export function getCouponRules() {
  return request.get<CouponRule[]>('/admin/coupon-rules')
}

export function createCouponRule(payload: CouponRuleCreatePayload) {
  return request.post<CouponRuleCreatePayload, CouponRule>('/admin/coupon-rules', payload)
}

export function deleteCouponRule(ruleId: number) {
  return request.delete(`/admin/coupon-rules/${ruleId}`)
}

export function getCouponInstances(params?: CouponInstanceQuery) {
  return request.get<CouponInstance[]>('/admin/coupon-instances', { params })
}

export function getCouponInstancesByTemplate(templateId: number) {
  return request.get<CouponInstance[]>(`/admin/coupons/${templateId}/instances`)
}

export function getCouponExchangesByTemplate(templateId: number) {
  return request.get<VoucherExchangeRecord[]>(`/admin/coupons/${templateId}/exchanges`)
}

export function updateVoucherExchangeStatus(exchangeId: number, payload: VoucherExchangeStatusPayload) {
  return request.post<VoucherExchangeStatusPayload, void>(`/admin/voucher-exchanges/${exchangeId}/status`, payload)
}

export function manualIssueCoupon(payload: CouponManualIssuePayload) {
  return request.post<CouponManualIssuePayload, CouponManualIssueResult>('/admin/coupon-instances/manual-issue', payload)
}
