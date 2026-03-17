import request from '@/utils/request'
import type { BillGeneratePayload, GenerateResult } from '@/types/billing'

export function generatePropertyBill(payload: BillGeneratePayload) {
  return request.post<never, GenerateResult>('/admin/bills/generate/property', payload)
}

export function generateWaterBill(payload: BillGeneratePayload) {
  return request.post<never, GenerateResult>('/admin/bills/generate/water', payload)
}
