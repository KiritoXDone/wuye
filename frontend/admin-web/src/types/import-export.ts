export interface BillImportCreatePayload {
  fileUrl: string
}

export interface ImportBatch {
  id: number
  batchNo: string
  importType: string
  fileUrl: string
  status: string
  totalCount: number
  successCount: number
  failCount: number
}

export interface ImportRowError {
  id: number
  batchId: number
  rowNo: number
  errorCode: string
  errorMessage: string
  rawData: string
}

export interface BillExportCreatePayload {
  periodYear?: number
  periodMonth?: number
  feeType?: string
  status?: string
}

export interface ExportJob {
  id: number
  exportType: string
  requestJson: string
  fileUrl?: string
  status: string
  expiredAt?: string
}
