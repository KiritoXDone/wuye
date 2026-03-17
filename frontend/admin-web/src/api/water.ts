import request from '@/utils/request'
import type { WaterMeter, WaterMeterPayload, WaterReading, WaterReadingPayload } from '@/types/water'

export function getWaterReadings(params: { periodYear?: number; periodMonth?: number }) {
  return request.get<never, WaterReading[]>('/admin/water-readings', { params })
}

export function createWaterMeter(payload: WaterMeterPayload) {
  return request.post<never, WaterMeter>('/admin/water-meters', payload)
}

export function createWaterReading(payload: WaterReadingPayload) {
  return request.post('/admin/water-readings', payload)
}
