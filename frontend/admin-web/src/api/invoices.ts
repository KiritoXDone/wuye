import request from '@/utils/request'
import type { InvoiceApplication, InvoiceProcessPayload } from '@/types/invoice'

export function processInvoiceApplication(applicationId: number, payload: InvoiceProcessPayload) {
  return request.post<InvoiceProcessPayload, InvoiceApplication>(`/admin/invoices/applications/${applicationId}/process`, payload)
}
