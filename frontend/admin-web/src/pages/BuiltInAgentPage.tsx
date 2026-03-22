import { useEffect, useRef, useState } from 'react'
import { Bot, History, MessageSquarePlus, Send } from 'lucide-react'

import { confirmAgentCommand, getAgentConversation, listAgentConversations, streamAgentConversation } from '@/api/agent'
import type { AgentCommandExecution, AgentConversationListItem, AgentConversationMessage } from '@/types/agent'

function toEntries(record: Record<string, unknown> | undefined) {
  if (!record) return []
  return Object.entries(record).filter(([, value]) => value !== null && value !== undefined && `${value}` !== '')
}

function formatValue(value: unknown) {
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'object' && value) return JSON.stringify(value)
  return String(value)
}

function payloadParts(payload?: Record<string, unknown>) {
  const summary = typeof payload?.summary === 'string' ? payload.summary : ''
  const warnings = Array.isArray(payload?.warnings) ? payload.warnings.map((item) => String(item)) : []
  const parsedArguments = payload?.parsedArguments && typeof payload.parsedArguments === 'object'
    ? (payload.parsedArguments as Record<string, unknown>)
    : undefined
  const resolvedContext = payload?.resolvedContext && typeof payload.resolvedContext === 'object'
    ? (payload.resolvedContext as Record<string, unknown>)
    : undefined
  const suggestions = Array.isArray(payload?.suggestions) ? payload.suggestions.map((item) => String(item)) : []
  const result = payload?.result
  return { summary, warnings, parsedArguments, resolvedContext, suggestions, result }
}

function resultText(result: AgentCommandExecution) {
  return result.summary || '已完成操作。'
}

export default function BuiltInAgentPage() {
  const [sessionId, setSessionId] = useState('')
  const [prompt, setPrompt] = useState('')
  const [messages, setMessages] = useState<AgentConversationMessage[]>([])
  const [conversationList, setConversationList] = useState<AgentConversationListItem[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [chatLoading, setChatLoading] = useState(false)
  const [confirmLoading, setConfirmLoading] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const listRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    void loadConversationList()
  }, [])

  function scrollToBottom() {
    requestAnimationFrame(() => {
      listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
    })
  }

  async function loadConversationList() {
    setListLoading(true)
    try {
      const page = await listAgentConversations({ pageNo: 1, pageSize: 20 })
      setConversationList(page.list || [])
    } finally {
      setListLoading(false)
    }
  }

  async function openConversation(targetSessionId: string) {
    setError('')
    const conversation = await getAgentConversation(targetSessionId)
    setSessionId(conversation.sessionId)
    setMessages((conversation.messages || []).map((item) => ({ ...item, streaming: false })))
    setMessage('')
    setPrompt('')
    scrollToBottom()
  }

  function resetConversation() {
    setSessionId('')
    setMessages([])
    setPrompt('')
    setMessage('')
    setError('')
  }

  async function handleSend(nextPrompt?: string) {
    const finalPrompt = (nextPrompt ?? prompt).trim()
    if (!finalPrompt) {
      setError('请输入消息')
      return
    }
    const userMessage: AgentConversationMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      mode: 'CHAT',
      content: finalPrompt,
      confirmationRequired: false,
      streaming: false,
    }

    setChatLoading(true)
    setError('')
    setMessage('')
    setPrompt('')
    setMessages((current) => [...current, userMessage])
    scrollToBottom()

    try {
      await streamAgentConversation(finalPrompt, sessionId || undefined, {
        onSession: (nextSessionId) => {
          if (nextSessionId) setSessionId(nextSessionId)
        },
        onMessageStart: (streamMessage) => {
          setMessages((current) => [...current, streamMessage])
          scrollToBottom()
        },
        onMessageDelta: (messageId, _delta, content) => {
          setMessages((current) => current.map((item) => (item.id === messageId ? { ...item, content, streaming: true } : item)))
          scrollToBottom()
        },
        onMessageComplete: (streamMessage) => {
          setMessages((current) => current.map((item) => (item.id === streamMessage.id ? { ...item, ...streamMessage, streaming: false } : item)))
          scrollToBottom()
        },
        onCommandPreview: (commandMessage) => {
          setMessages((current) => [...current, commandMessage])
          scrollToBottom()
        },
        onDone: () => {
          setChatLoading(false)
          void loadConversationList()
        },
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送失败')
      setChatLoading(false)
    } finally {
      setChatLoading(false)
    }
  }

  async function handleConfirm(messageItem: AgentConversationMessage) {
    if (!messageItem.confirmationToken) {
      return
    }
    setConfirmLoading(messageItem.confirmationToken)
    setError('')
    setMessage('')
    try {
      const result = await confirmAgentCommand(messageItem.confirmationToken)
      const resultMessage: AgentConversationMessage = {
        id: `result-${result.commandId}`,
        role: 'assistant',
        mode: 'RESULT',
        content: resultText(result),
        action: result.action,
        commandId: result.commandId,
        riskLevel: result.riskLevel,
        confirmationRequired: false,
        payload: {
          summary: result.summary,
          result: result.result,
        },
      }
      setMessages((current) => current.map((item) => (
        item.confirmationToken === messageItem.confirmationToken ? { ...item, confirmationRequired: false } : item
      )).concat(resultMessage))
      setMessage(result.summary)
      void loadConversationList()
      scrollToBottom()
    } catch (err) {
      setError(err instanceof Error ? err.message : '确认失败')
    } finally {
      setConfirmLoading('')
    }
  }

  function renderStructured(item: AgentConversationMessage) {
    const { summary, warnings, parsedArguments, resolvedContext, result } = payloadParts(item.payload)
    const parsedEntries = toEntries(parsedArguments)
    const contextEntries = toEntries(resolvedContext)
    const resultEntries = result && typeof result === 'object' && !Array.isArray(result) ? toEntries(result as Record<string, unknown>) : []

    return (
      <div className="mt-3 space-y-2 text-xs text-slate-600">
        {summary ? <div className="rounded-2xl bg-slate-50 px-3 py-2 leading-5 text-slate-700">{summary}</div> : null}
        {warnings.length ? (
          <div className="rounded-2xl border border-amber-200 bg-amber-50 px-3 py-2 text-amber-700">
            {warnings.map((warning) => <div key={warning}>{warning}</div>)}
          </div>
        ) : null}
        {parsedEntries.length ? (
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="mb-2 text-[11px] font-medium uppercase tracking-wide text-slate-400">识别参数</div>
            <div className="grid gap-2 sm:grid-cols-2">
              {parsedEntries.map(([key, value]) => (
                <div key={key}>
                  <div className="text-[11px] text-slate-400">{key}</div>
                  <div className="text-slate-700">{formatValue(value)}</div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
        {contextEntries.length ? (
          <div className="rounded-2xl border border-slate-200 bg-white px-3 py-2">
            <div className="mb-2 text-[11px] font-medium uppercase tracking-wide text-slate-400">当前上下文</div>
            <div className="grid gap-2 sm:grid-cols-2">
              {contextEntries.map(([key, value]) => (
                <div key={key}>
                  <div className="text-[11px] text-slate-400">{key}</div>
                  <div className="text-slate-700">{formatValue(value)}</div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
        {resultEntries.length ? (
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-3 py-2">
            <div className="mb-2 text-[11px] font-medium uppercase tracking-wide text-emerald-500">执行结果</div>
            <div className="grid gap-2 sm:grid-cols-2">
              {resultEntries.map(([key, value]) => (
                <div key={key}>
                  <div className="text-[11px] text-emerald-500">{key}</div>
                  <div className="text-emerald-800">{formatValue(value)}</div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    )
  }

  function renderMessage(item: AgentConversationMessage, index: number) {
    const isUser = item.role === 'user'
    return (
      <div key={item.id || `${item.role}-${index}`} className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
        <div className={`max-w-[78%] rounded-[24px] px-4 py-3 text-sm ${isUser ? 'bg-slate-950 text-white' : 'border border-slate-200 bg-white text-slate-800'}`}>
          <div className="whitespace-pre-wrap leading-6">{item.content}{item.streaming ? <span className="ml-1 inline-block h-4 w-2 animate-pulse rounded-full bg-slate-300 align-middle" /> : null}</div>
          {!isUser && (item.action || item.riskLevel || (item.mode && item.mode !== 'CHAT' && item.mode !== 'RESULT')) ? (
            <div className="mt-2 text-[11px] text-slate-400">{item.action || item.mode}{(item.action || item.mode) && item.riskLevel ? ' · ' : ''}{item.riskLevel || ''}</div>
          ) : null}
          {!isUser ? renderStructured(item) : null}
          {item.confirmationRequired && item.confirmationToken ? (
            <div className="mt-3 flex justify-end">
              <button type="button" className="rounded-full border border-rose-200 px-4 py-1.5 text-xs font-medium text-rose-600 transition hover:bg-rose-50" onClick={() => void handleConfirm(item)} disabled={confirmLoading === item.confirmationToken}>
                {confirmLoading === item.confirmationToken ? '确认中' : '确认'}
              </button>
            </div>
          ) : null}
        </div>
      </div>
    )
  }

  return (
    <div className="grid min-h-[calc(100vh-8rem)] gap-4 pb-2 lg:grid-cols-[320px_minmax(0,1fr)]">
      <aside className="panel flex min-h-0 flex-col overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <History className="h-4 w-4" />历史会话
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={resetConversation}>
            <MessageSquarePlus className="h-4 w-4" />新会话
          </button>
        </div>
        <div className="flex-1 space-y-2 overflow-y-auto p-3">
          {listLoading ? <div className="px-3 py-6 text-center text-sm text-slate-400">加载中</div> : null}
          {!listLoading && !conversationList.length ? <div className="px-3 py-6 text-center text-sm text-slate-400">暂无历史会话</div> : null}
          {conversationList.map((item) => (
            <button
              key={item.sessionId}
              type="button"
              className={`w-full rounded-2xl border px-3 py-3 text-left transition ${sessionId === item.sessionId ? 'border-slate-900 bg-slate-900 text-white' : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-white'}`}
              onClick={() => void openConversation(item.sessionId)}
            >
              <div className="truncate text-sm font-medium">{item.title || '未命名会话'}</div>
              <div className={`mt-1 line-clamp-2 text-xs ${sessionId === item.sessionId ? 'text-slate-200' : 'text-slate-500'}`}>{item.lastMessagePreview || '暂无摘要'}</div>
            </button>
          ))}
        </div>
      </aside>

      <div className="flex min-h-0 flex-col gap-4">
        <section className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-5 py-4">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2"><Bot className="h-5 w-5 text-slate-900" /></div>
            <div>
              <div className="text-base font-semibold text-slate-950">物业助手</div>
              <div className="text-xs text-slate-500">{chatLoading ? '正在回复' : sessionId ? '会话中' : '新对话'}</div>
            </div>
          </div>
        </section>

        {error ? <div className="rounded-full bg-rose-50 px-4 py-2 text-center text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-full bg-emerald-50 px-4 py-2 text-center text-sm text-emerald-700">{message}</div> : null}

        <div ref={listRef} className="flex-1 overflow-y-auto rounded-3xl border border-slate-200 bg-slate-50 p-4">
          <div className="space-y-3">
            {messages.length ? messages.map(renderMessage) : <div className="py-20 text-center text-sm text-slate-400">开始对话</div>}
          </div>
        </div>

        <div className="space-y-3 rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-end gap-3">
            <textarea
              className="textarea min-h-[88px] flex-1"
              rows={3}
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="输入消息"
            />
            <button type="button" className="btn-primary gap-2" onClick={() => void handleSend()} disabled={chatLoading}>
              <Send className="h-4 w-4" />
              {chatLoading ? '发送中' : '发送'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
