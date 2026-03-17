import request from '@/utils/request'
import type {
  BillExportCreatePayload,
  BillImportCreatePayload,
  ExportJob,
  ImportBatch,
  ImportRowError,
} from '@/types/import-export'

export function createBillImport(payload: BillImportCreatePayload) {
  return request.post<never, ImportBatch>('/admin/imports/bills', payload)
}

export function getImportBatch(batchId: number) {
  return request.get<never, ImportBatch>(`/admin/imports/${batchId}`)
}

export function getImportBatchErrors(batchId: number) {
  return request.get<never, ImportRowError[]>(`/admin/imports/${batchId}/errors`)
}

export function createBillExport(payload: BillExportCreatePayload) {
  return request.post<never, ExportJob>('/admin/exports/bills', payload)
}

export function getExportJob(jobId: number) {
  return request.get<never, ExportJob>(`/admin/exports/${jobId}`)
}
