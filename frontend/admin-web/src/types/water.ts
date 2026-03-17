export interface WaterMeterPayload {
  roomId: number
  meterNo?: string
  installAt?: string
}

export interface WaterMeter {
  id: number
  roomId: number
  meterNo?: string
  installAt?: string
  status: number
}

export interface WaterReading {
  id: number
  roomId: number
  roomLabel: string
  periodYear: number
  periodMonth: number
  prevReading: number | string
  currReading: number | string
  usageAmount: number | string
  readAt: string
  status: string
}

export interface WaterReadingPayload {
  roomId: number
  year: number
  month: number
  prevReading: number
  currReading: number
  readAt: string
  photoUrl?: string
  remark?: string
}
