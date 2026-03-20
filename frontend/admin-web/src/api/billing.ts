import request from '@/utils/request'
import type { BillGeneratePayload, GenerateResult } from '@/types/billing'

export function generatePropertyBill(payload: BillGeneratePayload) {
  return request.post<BillGeneratePayload, GenerateResult>('/admin/bills/generate/property-yearly', payload)
}

export function generateWaterBill(payload: BillGeneratePayload) {
  return request.post<BillGeneratePayload, GenerateResult>('/admin/bills/generate/water', payload)
}
