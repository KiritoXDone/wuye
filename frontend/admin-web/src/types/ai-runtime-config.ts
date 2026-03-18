export interface AiRuntimeConfig {
  enabled: boolean
  apiBaseUrl: string
  provider: string
  model: string
  apiKeyMasked: string
  timeoutMs: number
  maxTokens: number
  temperature: number
}

export interface AiRuntimeConfigUpdatePayload {
  enabled: boolean
  apiBaseUrl: string
  provider: string
  model: string
  apiKey?: string
  timeoutMs: number
  maxTokens: number
  temperature: number
}
