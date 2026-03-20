import request from '@/utils/request'
import type { WaterAlert, WaterAlertQuery } from '@/types/water-alert'

export function getWaterAlerts(params: WaterAlertQuery) {
  return request.get<WaterAlert[]>('/admin/water-alerts', { params })
}
