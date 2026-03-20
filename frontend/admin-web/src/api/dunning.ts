import request from '@/utils/request'
import type { DunningLog, DunningTask, DunningTriggerPayload } from '@/types/dunning'

export function triggerDunning(payload: DunningTriggerPayload) {
  return request.post<DunningTriggerPayload, DunningTask[]>('/admin/dunning/trigger', payload)
}

export function getDunningTasks() {
  return request.get<DunningTask[]>('/admin/dunning/tasks')
}

export function getDunningLogs(billId: number) {
  return request.get<DunningLog[]>(`/admin/dunning/bills/${billId}/logs`)
}
