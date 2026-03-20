export interface BillGeneratePayload {
  communityId: number
  year: number
  month?: number
  overwriteStrategy?: string
}

export interface GenerateResult {
  generatedCount: number
}
