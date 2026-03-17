export interface InvoiceApplication {
  id: number
  applicationNo: string
  billId: number
  payOrderNo: string
  invoiceTitle: string
  taxNo?: string
  status: string
  remark?: string
  appliedAt?: string
  processedAt?: string
}

export interface InvoiceProcessPayload {
  status: string
  remark?: string
}
