import request from '@/utils/request'
import type { DunningLog, DunningTask, DunningTriggerPayload } from '@/types/dunning'

export function triggerDunning(payload: DunningTriggerPayload) {
  return request.post<never, DunningTask[]>('/admin/dunning/trigger', payload)
}

export function getDunningTasks() {
  return request.get<never, DunningTask[]>('/admin/dunning/tasks')
}

export function getDunningLogs(billId: number) {
  return request.get<never, DunningLog[]>(`/admin/dunning/bills/${billId}/logs`)
}
