import type { AgentCommandExecution, AgentConversationListItem, AgentConversationMessage } from '../../types/agent'
import { confirmAgentConversation, getAgentConversation, listAgentConversations, streamAgentConversation } from '../../services/agent'
import { hasAuthSession } from '../../utils/auth'

function payloadParts(payload?: Record<string, unknown>) {
  const summary = typeof payload?.summary === 'string' ? payload.summary : ''
  const warnings = Array.isArray(payload?.warnings) ? payload.warnings.map((item) => String(item)) : []
  const parsedArguments = payload?.parsedArguments && typeof payload.parsedArguments === 'object'
    ? Object.entries(payload.parsedArguments as Record<string, unknown>).filter(([, value]) => value !== null && value !== undefined && `${value}` !== '')
    : []
  const resolvedContext = payload?.resolvedContext && typeof payload.resolvedContext === 'object'
    ? Object.entries(payload.resolvedContext as Record<string, unknown>).filter(([, value]) => value !== null && value !== undefined && `${value}` !== '')
    : []
  const resultEntries = payload?.result && typeof payload.result === 'object' && !Array.isArray(payload.result)
    ? Object.entries(payload.result as Record<string, unknown>).filter(([, value]) => value !== null && value !== undefined && `${value}` !== '')
    : []
  return { summary, warnings, parsedArguments, resolvedContext, resultEntries }
}

function decorateMessage(message: AgentConversationMessage) {
  const payload = payloadParts(message.payload)
  return {
    ...message,
    metaText: [message.action || '', message.riskLevel || ''].filter(Boolean).join(' · '),
    summaryText: payload.summary,
    warningList: payload.warnings,
    parsedArgumentEntries: payload.parsedArguments.map(([label, value]) => ({ label, value: String(value) })),
    resolvedContextEntries: payload.resolvedContext.map(([label, value]) => ({ label, value: String(value) })),
    resultEntries: payload.resultEntries.map(([label, value]) => ({ label, value: String(value) })),
  }
}

function buildResultMessage(result: AgentCommandExecution): AgentConversationMessage {
  return {
    id: `result-${result.commandId}`,
    role: 'assistant',
    mode: 'RESULT',
    content: result.summary || '已完成操作。',
    action: result.action,
    commandId: result.commandId,
    riskLevel: result.riskLevel,
    confirmationRequired: false,
    payload: {
      summary: result.summary,
      result: typeof result.result === 'object' && result.result ? result.result as Record<string, unknown> : undefined,
    },
    streaming: false,
  }
}

Page({
  data: {
    sessionId: '',
    inputMessage: '',
    loading: false,
    streaming: false,
    errorMessage: '',
    successMessage: '',
    scrollIntoView: '',
    messages: [] as Array<ReturnType<typeof decorateMessage>>,
    recentSessions: [] as AgentConversationListItem[],
    loadingSessions: false,
  },

  streamTask: null as { abort(): void } | null,
  chunkFlushTimer: null as number | null,
  pendingContent: '' as string,
  pendingMessageId: '' as string,

  async onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    await this.loadRecentSessions()
    const app = getApp<IAppOption>()
    const contextPrompt = app.globalData.agentContextPrompt || ''
    if (contextPrompt) {
      app.globalData.agentContextPrompt = ''
      this.setData({ inputMessage: contextPrompt })
      this.handleSend()
    }
  },

  onUnload() {
    this.abortStream()
  },

  async loadRecentSessions() {
    this.setData({ loadingSessions: true })
    try {
      const page = await listAgentConversations({ pageNo: 1, pageSize: 5 })
      this.setData({ recentSessions: page.list || [] })
    } finally {
      this.setData({ loadingSessions: false })
    }
  },

  async openConversation(event: WechatMiniprogram.BaseEvent) {
    const sessionId = String(event.currentTarget.dataset.sessionId || '')
    if (!sessionId) return
    try {
      const conversation = await getAgentConversation(sessionId)
      this.setData({
        sessionId: conversation.sessionId,
        messages: (conversation.messages || []).map((item) => decorateMessage({ ...item, streaming: false })),
        scrollIntoView: conversation.messages?.length ? `message-${conversation.messages.length - 1}` : '',
        errorMessage: '',
        successMessage: '',
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '加载会话失败' })
    }
  },

  resetConversation() {
    this.setData({
      sessionId: '',
      inputMessage: '',
      errorMessage: '',
      successMessage: '',
      messages: [],
      scrollIntoView: '',
    })
  },

  handleInput(event: WechatMiniprogram.Input) {
    this.setData({ inputMessage: event.detail.value })
  },

  handleQuickPrompt(event: WechatMiniprogram.BaseEvent) {
    const prompt = String(event.currentTarget.dataset.prompt || '')
    if (!prompt || this.data.streaming) {
      return
    }
    this.setData({ inputMessage: prompt })
    this.handleSend()
  },

  updateMessages(messages: AgentConversationMessage[]) {
    const decorated = messages.map((item) => decorateMessage(item))
    const scrollIntoView = decorated.length ? `message-${decorated.length - 1}` : ''
    this.setData({ messages: decorated, scrollIntoView })
  },

  flushPendingChunk() {
    if (!this.pendingMessageId) return
    const messageId = this.pendingMessageId
    const nextContent = this.pendingContent
    const messages = (this.data.messages as Array<ReturnType<typeof decorateMessage>>).map((item) => (
      item.id === messageId ? decorateMessage({ ...item, content: nextContent, streaming: true }) : item
    ))
    const scrollIntoView = messages.length ? `message-${messages.length - 1}` : ''
    this.setData({ messages, scrollIntoView })
    this.chunkFlushTimer = null
  },

  scheduleChunkFlush(messageId: string, content: string) {
    this.pendingMessageId = messageId
    this.pendingContent = content
    if (this.chunkFlushTimer != null) {
      return
    }
    this.chunkFlushTimer = setTimeout(() => this.flushPendingChunk(), 80) as unknown as number
  },

  abortStream() {
    if (this.streamTask) {
      this.streamTask.abort()
      this.streamTask = null
    }
    if (this.chunkFlushTimer != null) {
      clearTimeout(this.chunkFlushTimer)
      this.chunkFlushTimer = null
    }
    this.pendingContent = ''
    this.pendingMessageId = ''
  },

  async handleSend() {
    const finalPrompt = this.data.inputMessage.trim()
    if (!finalPrompt) {
      this.setData({ errorMessage: '请输入问题或操作指令。', successMessage: '' })
      return
    }

    this.abortStream()
    const userMessage: AgentConversationMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      mode: 'CHAT',
      content: finalPrompt,
      confirmationRequired: false,
      streaming: false,
    }

    this.setData({
      loading: true,
      streaming: true,
      errorMessage: '',
      successMessage: '',
      inputMessage: '',
    })
    this.updateMessages((this.data.messages as Array<ReturnType<typeof decorateMessage>>).concat(decorateMessage(userMessage)))

    this.streamTask = streamAgentConversation(finalPrompt, this.data.sessionId || undefined, {
      onSession: (nextSessionId) => {
        if (nextSessionId) {
          this.setData({ sessionId: nextSessionId })
        }
      },
      onMessageStart: (message) => {
        this.updateMessages((this.data.messages as Array<ReturnType<typeof decorateMessage>>).concat(decorateMessage(message)))
      },
      onMessageDelta: (messageId, _delta, content) => {
        this.scheduleChunkFlush(messageId, content)
      },
      onMessageComplete: (message) => {
        this.flushPendingChunk()
        const messages = (this.data.messages as Array<ReturnType<typeof decorateMessage>>).map((item) => (
          item.id === message.id ? decorateMessage({ ...message, streaming: false }) : item
        ))
        this.setData({ messages, scrollIntoView: messages.length ? `message-${messages.length - 1}` : '' })
      },
      onCommandPreview: (message) => {
        this.updateMessages((this.data.messages as Array<ReturnType<typeof decorateMessage>>).concat(decorateMessage(message)))
      },
      onDone: () => {
        this.flushPendingChunk()
        this.streamTask = null
        this.setData({ loading: false, streaming: false })
        void this.loadRecentSessions()
      },
      onError: (errorMessage) => {
        this.abortStream()
        this.setData({ loading: false, streaming: false, errorMessage })
      }
    })
  },

  async handleConfirm(event: WechatMiniprogram.BaseEvent) {
    const token = String(event.currentTarget.dataset.token || '')
    if (!token) {
      return
    }
    this.setData({ loading: true, errorMessage: '', successMessage: '' })
    try {
      const result = await confirmAgentConversation(token)
      const resultMessage = decorateMessage(buildResultMessage(result))
      const nextMessages = (this.data.messages as Array<ReturnType<typeof decorateMessage>>).map((item) => (
        item.confirmationToken === token ? { ...item, confirmationRequired: false } : item
      )).concat(resultMessage)
      this.setData({
        messages: nextMessages,
        successMessage: `已执行：${result.summary}`,
        scrollIntoView: nextMessages.length ? `message-${nextMessages.length - 1}` : '',
      })
      await this.loadRecentSessions()
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '确认失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
