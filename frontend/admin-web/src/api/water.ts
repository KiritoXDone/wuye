import request from '@/utils/request'
import type { WaterMeter, WaterMeterPayload, WaterReading, WaterReadingCreateResult, WaterReadingPayload } from '@/types/water'

export function getWaterReadings(params: { periodYear?: number; periodMonth?: number }) {
  return request.get<WaterReading[]>('/admin/water-readings', { params })
}

export function createWaterMeter(payload: WaterMeterPayload) {
  return request.post<WaterMeterPayload, WaterMeter>('/admin/water-meters', payload)
}

export function createWaterReading(payload: WaterReadingPayload) {
  return request.post<WaterReadingPayload, WaterReadingCreateResult>('/admin/water-readings', payload)
}

export function deleteWaterReading(readingId: number) {
  return request.delete(`/admin/water-readings/${readingId}`)
}
