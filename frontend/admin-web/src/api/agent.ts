import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import type { AgentAdminBillStats, AgentCommandExecution, AgentCommandPreview, AgentConversation, AgentConversationListItem, AgentConversationMessage } from '@/types/agent'

export interface AgentStreamHandlers {
  onSession?: (sessionId: string) => void
  onMessageStart?: (message: AgentConversationMessage) => void
  onMessageDelta?: (messageId: string, delta: string, content: string) => void
  onMessageComplete?: (message: AgentConversationMessage) => void
  onCommandPreview?: (message: AgentConversationMessage) => void
  onDone?: () => void
}

export function getAdminAgentBillStats(params: { periodYear?: number; periodMonth?: number }) {
  return request.get<AgentAdminBillStats>('/ai/agent/admin/bill-stats', { params })
}

export function previewAgentCommand(prompt: string) {
  return request.post<{ prompt: string }, AgentCommandPreview>('/ai/agent/commands/preview', { prompt })
}

export function confirmAgentCommand(confirmationToken: string) {
  return request.post<{ confirmationToken: string }, AgentCommandExecution>('/ai/agent/commands/confirm', { confirmationToken })
}

export function getAgentCommand(commandId: string) {
  return request.get<AgentCommandExecution>(`/ai/agent/commands/${commandId}`)
}

export function sendAgentConversation(message: string, sessionId?: string) {
  return request.post<{ sessionId?: string; message: string }, AgentConversation>('/ai/agent/conversation', { sessionId, message })
}

export function getAgentConversation(sessionId: string) {
  return request.get<AgentConversation>(`/ai/agent/conversation/${sessionId}`)
}

export function listAgentConversations(params?: { pageNo?: number; pageSize?: number }) {
  return request.get<{ list: AgentConversationListItem[]; pageNo: number; pageSize: number; total: number }>('/ai/agent/conversation/sessions', { params })
}

export async function streamAgentConversation(message: string, sessionId: string | undefined, handlers: AgentStreamHandlers) {
  const token = useAuthStore.getState().accessToken
  const response = await fetch('/api/v1/ai/agent/conversation/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ sessionId, message }),
  })

  if (!response.ok) {
    throw new Error(`请求失败(${response.status})`)
  }
  if (!response.body) {
    throw new Error('流式响应不可用')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let eventName = 'message'

  function emitEvent(name: string, rawData: string) {
    if (!rawData.trim()) return
    const payload = JSON.parse(rawData)
    if (name === 'session') {
      handlers.onSession?.(String(payload.sessionId || ''))
      return
    }
    if (name === 'message-start') {
      handlers.onMessageStart?.({
        id: String(payload.messageId || ''),
        role: String(payload.role || 'assistant'),
        mode: payload.mode,
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
        mode: payload.mode,
        content: String(payload.content || ''),
        action: payload.action,
        commandId: payload.commandId,
        riskLevel: payload.riskLevel,
        confirmationRequired: Boolean(payload.confirmationRequired),
        confirmationToken: payload.confirmationToken,
        payload: payload.payload,
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
      throw new Error(String(payload.message || '流式对话失败'))
    }
  }

  function consumeBuffer() {
    const normalized = buffer.replace(/\r\n/g, '\n')
    const blocks = normalized.split('\n\n')
    buffer = blocks.pop() || ''
    for (const block of blocks) {
      const lines = block.split('\n')
      let data = ''
      eventName = 'message'
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          data += line.slice(5).trim()
        }
      }
      emitEvent(eventName, data)
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    consumeBuffer()
  }
  buffer += decoder.decode()
  consumeBuffer()
}