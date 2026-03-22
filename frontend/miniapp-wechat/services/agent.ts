import type { AgentCommandExecution, AgentConversation, AgentConversationListItem, AgentConversationMessage, AgentResidentBillSummary } from '../types/agent'
import { request } from '../utils/request'
import { API_BASE_URL } from '../config/env'
import { getAccessToken } from '../utils/auth'

export interface AgentStreamHandlers {
  onSession?: (sessionId: string) => void
  onMessageStart?: (message: AgentConversationMessage) => void
  onMessageDelta?: (messageId: string, delta: string, content: string) => void
  onMessageComplete?: (message: AgentConversationMessage) => void
  onCommandPreview?: (message: AgentConversationMessage) => void
  onDone?: () => void
  onError?: (message: string) => void
}

export function getResidentBillSummary() {
  return request<AgentResidentBillSummary>({
    url: '/api/v1/ai/agent/me/bill-summary'
  })
}

export function sendAgentConversation(message: string, sessionId?: string) {
  return request<AgentConversation>({
    url: '/api/v1/ai/agent/conversation',
    method: 'POST',
    data: { sessionId, message }
  })
}

export function confirmAgentConversation(confirmationToken: string) {
  return request<AgentCommandExecution>({
    url: '/api/v1/ai/agent/commands/confirm',
    method: 'POST',
    data: { confirmationToken }
  })
}

export function getAgentConversation(sessionId: string) {
  return request<AgentConversation>({
    url: `/api/v1/ai/agent/conversation/${sessionId}`
  })
}

export function listAgentConversations(data?: { pageNo?: number; pageSize?: number }) {
  return request<{ list: AgentConversationListItem[]; pageNo: number; pageSize: number; total: number }>({
    url: '/api/v1/ai/agent/conversation/sessions',
    data
  })
}

export function streamAgentConversation(message: string, sessionId: string | undefined, handlers: AgentStreamHandlers) {
  const token = getAccessToken()
  if (!token) {
    handlers.onError?.('登录状态已失效')
    return { abort() {} }
  }

  let buffer = ''
  const decoder = new TextDecoder('utf-8')

  function emitEvent(name: string, rawData: string) {
    if (!rawData.trim()) return
    const payload = JSON.parse(rawData) as Record<string, unknown>
    if (name === 'session') {
      handlers.onSession?.(String(payload.sessionId || ''))
      return
    }
    if (name === 'message-start') {
      handlers.onMessageStart?.({
        id: String(payload.messageId || ''),
        role: String(payload.role || 'assistant'),
        mode: String(payload.mode || 'CHAT'),
        content: '',
        confirmationRequired: false,
        streaming: true,
      })
      return
    }
    if (name === 'message-delta') {
      handlers.onMessageDelta?.(String(payload.messageId || ''), String(payload.delta || ''), String(payload.content || ''))
      return
    }
    if (name === 'message-complete' || name === 'command-preview') {
      const message: AgentConversationMessage = {
        id: String(payload.messageId || ''),
        role: String(payload.role || 'assistant'),
        mode: String(payload.mode || 'CHAT'),
        content: String(payload.content || ''),
        action: payload.action ? String(payload.action) : undefined,
        commandId: payload.commandId ? String(payload.commandId) : undefined,
        riskLevel: payload.riskLevel ? String(payload.riskLevel) : undefined,
        confirmationRequired: Boolean(payload.confirmationRequired),
        confirmationToken: payload.confirmationToken ? String(payload.confirmationToken) : undefined,
        payload: (payload.payload as Record<string, unknown>) || undefined,
        streaming: false,
      }
      if (name === 'command-preview') {
        handlers.onCommandPreview?.(message)
      } else {
        handlers.onMessageComplete?.(message)
      }
      return
    }
    if (name === 'done') {
      handlers.onDone?.()
      return
    }
    if (name === 'error') {
      handlers.onError?.(String(payload.message || '流式对话失败'))
    }
  }

  function consumeBuffer() {
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''
    blocks.forEach((block) => {
      const lines = block.split('\n')
      let eventName = 'message'
      let data = ''
      lines.forEach((line) => {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          data += line.slice(5).trim()
        }
      })
      emitEvent(eventName, data)
    })
  }

  const requestTask = wx.request({
    url: `${API_BASE_URL}/api/v1/ai/agent/conversation/stream`,
    method: 'POST',
    enableChunked: true,
    responseType: 'arraybuffer',
    timeout: 30000,
    data: { sessionId, message },
    header: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    success: () => {
      consumeBuffer()
      handlers.onDone?.()
    },
    fail: () => {
      handlers.onError?.('网络请求失败，请确认后端已启动并支持流式输出')
    },
  })

  requestTask.onChunkReceived((result) => {
    buffer += decoder.decode(result.data, { stream: true })
    consumeBuffer()
  })

  return requestTask
}