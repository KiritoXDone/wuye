export interface AiRuntimeConfig {
  enabled: boolean
  apiBaseUrl: string
  model: string
  apiKeyMasked: string
}

export interface AiRuntimeConfigUpdatePayload {
  enabled: boolean
  apiBaseUrl: string
  model: string
  apiKey?: string
}
