import request from '@/utils/request'
import type { AiRuntimeConfig, AiRuntimeConfigUpdatePayload } from '@/types/ai-runtime-config'

export function getAiRuntimeConfig() {
  return request.get<AiRuntimeConfig>('/admin/agent/runtime-config')
}

export function updateAiRuntimeConfig(payload: AiRuntimeConfigUpdatePayload) {
  return request.put<AiRuntimeConfigUpdatePayload, AiRuntimeConfig>('/admin/agent/runtime-config', payload)
}
