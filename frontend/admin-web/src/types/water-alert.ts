export interface WaterAlertQuery {
  periodYear?: number
  periodMonth?: number
}

export interface WaterAlert {
  id: number
  readingId: number
  roomId: number
  roomLabel: string
  alertCode: string
  alertMessage: string
  thresholdValue?: number | string
  actualValue?: number | string
  status: string
  createdAt: string
}
