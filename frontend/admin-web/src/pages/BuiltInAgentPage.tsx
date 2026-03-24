import { useEffect, useRef, useState } from 'react'
import { Bot, History, MessageSquarePlus, Send } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

import { confirmAgentCommand, getAgentConversation, listAgentConversations, streamAgentConversation } from '@/api/agent'
import type { AgentCommandExecution, AgentConversationListItem, AgentConversationMessage } from '@/types/agent'

interface BuiltInAgentPageProps {
  embedded?: boolean
}

function payloadParts(payload?: Record<string, unknown>) {
  const summary = typeof payload?.summary === 'string' ? payload.summary : ''
  const warnings = Array.isArray(payload?.warnings) ? payload.warnings.map((item) => String(item)) : []
  const missingArguments = Array.isArray(payload?.missingArguments) ? payload.missingArguments.map((item) => String(item)) : []
  const resultSummary = typeof payload?.resultSummary === 'string' ? payload.resultSummary : ''
  const resultMarkdown = typeof payload?.resultMarkdown === 'string' ? payload.resultMarkdown : ''
  return { summary, warnings, missingArguments, resultSummary, resultMarkdown }
}

function fallbackResultText(result: AgentCommandExecution) {
  return result.resultSummary || result.summary || '我已经处理完成。'
}

function markdownClassName(isUser: boolean) {
  return [
    'prose prose-sm max-w-none',
    'prose-p:my-0 prose-p:leading-6',
    'prose-ul:my-2 prose-ol:my-2',
    'prose-li:my-0.5',
    'prose-pre:my-2 prose-pre:overflow-x-auto prose-pre:rounded-2xl prose-pre:px-4 prose-pre:py-3',
    'prose-code:rounded prose-code:px-1 prose-code:py-0.5 prose-code:before:content-none prose-code:after:content-none',
    'prose-strong:font-semibold',
    isUser
      ? 'prose-invert prose-headings:text-white prose-p:text-white prose-strong:text-white prose-code:bg-white/15 prose-code:text-white prose-pre:bg-white/10 prose-a:text-white'
      : 'prose-slate prose-headings:text-slate-900 prose-p:text-slate-700 prose-strong:text-slate-900 prose-code:bg-slate-100 prose-code:text-slate-900 prose-pre:bg-slate-950 prose-pre:text-slate-50 prose-a:text-sky-700',
  ].join(' ')
}

export default function BuiltInAgentPage({ embedded = false }: BuiltInAgentPageProps) {
  const [sessionId, setSessionId] = useState('')
  const [conversationTitle, setConversationTitle] = useState('')
  const [prompt, setPrompt] = useState('')
  const [messages, setMessages] = useState<AgentConversationMessage[]>([])
  const [conversationList, setConversationList] = useState<AgentConversationListItem[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [chatLoading, setChatLoading] = useState(false)
  const [confirmLoading, setConfirmLoading] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const listRef = useRef<HTMLDivElement | null>(null)
  const sessionIdRef = useRef('')

  useEffect(() => {
    sessionIdRef.current = sessionId
  }, [sessionId])

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
      const nextList = page.list || []
      setConversationList(nextList)
      if (sessionIdRef.current) {
        const active = nextList.find((item) => item.sessionId === sessionIdRef.current)
        if (active?.title) {
          setConversationTitle(active.title)
        }
      }
      return nextList
    } finally {
      setListLoading(false)
    }
  }

  async function openConversation(targetSessionId: string) {
    setError('')
    const conversation = await getAgentConversation(targetSessionId)
    setSessionId(conversation.sessionId)
    setConversationTitle(conversation.title || '')
    setMessages((conversation.messages || []).map((item) => ({ ...item, streaming: false })))
    setMessage('')
    setPrompt('')
    scrollToBottom()
  }

  function resetConversation() {
    setSessionId('')
    setConversationTitle('')
    setMessages([])
    setPrompt('')
    setMessage('')
    setError('')
  }

  async function refreshActiveConversationTitle(targetSessionId: string) {
    if (!targetSessionId) return
    try {
      const conversation = await getAgentConversation(targetSessionId)
      setConversationTitle(conversation.title || '')
    } catch {
      // ignore refresh failures
    }
  }

  async function handleSend(nextPrompt?: string) {
    const finalPrompt = (nextPrompt ?? prompt).trim()
    if (!finalPrompt) {
      setError('请输入消息。')
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
          if (nextSessionId) {
            setSessionId(nextSessionId)
          }
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
          const activeSessionId = sessionIdRef.current
          void loadConversationList()
          void refreshActiveConversationTitle(activeSessionId)
        },
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送失败。')
      setChatLoading(false)
    } finally {
      setChatLoading(false)
    }
  }

  async function handleConfirm(messageItem: AgentConversationMessage) {
    if (!messageItem.confirmationToken) return
    setConfirmLoading(messageItem.confirmationToken)
    setError('')
    setMessage('')
    try {
      const result = await confirmAgentCommand(messageItem.confirmationToken)
      const resultMessage: AgentConversationMessage = {
        id: `result-${result.commandId}`,
        role: 'assistant',
        mode: 'RESULT',
        content: fallbackResultText(result),
        action: result.action,
        commandId: result.commandId,
        riskLevel: result.riskLevel,
        confirmationRequired: false,
        payload: {
          summary: result.summary,
          resultSummary: result.resultSummary,
          resultMarkdown: result.resultMarkdown,
        },
      }
      setMessages((current) =>
        current
          .map((item) => (item.confirmationToken === messageItem.confirmationToken ? { ...item, confirmationRequired: false } : item))
          .concat(resultMessage),
      )
      setMessage(result.resultSummary || result.summary)
      void loadConversationList()
      void refreshActiveConversationTitle(sessionIdRef.current)
      scrollToBottom()
    } catch (err) {
      setError(err instanceof Error ? err.message : '确认失败。')
    } finally {
      setConfirmLoading('')
    }
  }

  function renderStructured(item: AgentConversationMessage) {
    const { summary, warnings, missingArguments, resultSummary, resultMarkdown } = payloadParts(item.payload)
    const helperText = resultSummary || summary

    return (
      <div className="mt-3 space-y-3 text-sm">
        {helperText ? (
          <div className="rounded-2xl bg-slate-50 px-3 py-3 leading-6 text-slate-700 dark:bg-slate-900 dark:text-slate-200">
            {helperText}
          </div>
        ) : null}

        {missingArguments.length ? (
          <div className="rounded-2xl border border-sky-200 bg-sky-50 px-3 py-3 text-sky-900 dark:border-sky-400/20 dark:bg-sky-500/15 dark:text-sky-100">
            <div className="leading-6">
              还缺少这些信息后我才能继续处理：{missingArguments.join('、')}。
            </div>
            <div className="mt-2 flex flex-wrap gap-2">
              {missingArguments.map((argument) => (
                <span key={argument} className="rounded-full bg-white px-2.5 py-1 text-xs font-medium text-sky-700 ring-1 ring-sky-200 dark:bg-slate-950 dark:text-sky-100 dark:ring-sky-400/25">
                  {argument}
                </span>
              ))}
            </div>
          </div>
        ) : null}

        {warnings.length ? (
          <div className="rounded-2xl border border-amber-200 bg-amber-50 px-3 py-3 text-amber-800 dark:border-amber-400/20 dark:bg-amber-500/15 dark:text-amber-100">
            {warnings.map((warning) => (
              <div key={warning} className="leading-6">
                {warning}
              </div>
            ))}
          </div>
        ) : null}

        {resultMarkdown ? (
          <div className="rounded-2xl border border-slate-200 bg-white px-3 py-3 dark:border-slate-700 dark:bg-slate-950">
            <div className={markdownClassName(false)}>
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  a: ({ node: _node, ...props }) => <a {...props} target="_blank" rel="noreferrer" />,
                }}
              >
                {resultMarkdown}
              </ReactMarkdown>
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
        <div
          className={`max-w-[88%] rounded-[24px] px-4 py-3 text-sm shadow-sm ${
            isUser
              ? 'bg-slate-950 text-white dark:bg-slate-100 dark:text-slate-950'
              : 'border border-slate-200 bg-white text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100'
          }`}
        >
          <div className={markdownClassName(isUser)}>
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                a: ({ node: _node, ...props }) => <a {...props} target="_blank" rel="noreferrer" />,
              }}
            >
              {item.content || ''}
            </ReactMarkdown>
          </div>
          {item.streaming ? <span className="mt-1 inline-block h-4 w-2 animate-pulse rounded-full bg-slate-300 align-middle dark:bg-slate-500" /> : null}
          {!isUser && item.mode === 'RESULT' ? renderStructured(item) : null}
          {!isUser && item.mode !== 'RESULT' && (item.confirmationRequired || payloadParts(item.payload).missingArguments.length || payloadParts(item.payload).warnings.length) ? renderStructured(item) : null}
          {item.confirmationRequired && item.confirmationToken ? (
            <div className="mt-3 flex justify-end">
              <button
                type="button"
                className="rounded-full border border-rose-200 px-4 py-1.5 text-xs font-medium text-rose-600 transition hover:bg-rose-50 dark:border-rose-400/20 dark:text-rose-100 dark:hover:bg-rose-500/15"
                onClick={() => void handleConfirm(item)}
                disabled={confirmLoading === item.confirmationToken}
              >
                {confirmLoading === item.confirmationToken ? '确认中...' : '确认执行'}
              </button>
            </div>
          ) : null}
        </div>
      </div>
    )
  }

  const activeTitle = conversationTitle || '新会话'
  const containerClassName = embedded
    ? 'grid h-full min-h-0 gap-3 lg:grid-cols-[280px_minmax(0,1fr)] lg:gap-4'
    : 'grid min-h-[calc(100vh-8rem)] gap-4 pb-2 lg:grid-cols-[320px_minmax(0,1fr)]'

  return (
    <div className={containerClassName}>
      <aside className="panel flex min-h-0 max-h-[240px] flex-col overflow-hidden lg:max-h-none">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-900 dark:text-slate-50">
            <History className="h-4 w-4" />
            历史会话
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={resetConversation}>
            <MessageSquarePlus className="h-4 w-4" />
            新会话
          </button>
        </div>
        <div className="flex-1 space-y-2 overflow-y-auto bg-slate-50/60 p-3">
          {listLoading ? <div className="px-3 py-6 text-center text-sm text-slate-400 dark:text-slate-500">加载中...</div> : null}
          {!listLoading && !conversationList.length ? <div className="px-3 py-6 text-center text-sm text-slate-400 dark:text-slate-500">暂无历史会话</div> : null}
          {conversationList.map((item) => (
            <button
              key={item.sessionId}
              type="button"
              className={`w-full rounded-2xl border px-3 py-3 text-left transition ${
                sessionId === item.sessionId
                  ? 'border-slate-950 bg-slate-950 text-white shadow-sm dark:border-slate-100 dark:bg-slate-100 dark:text-slate-950'
                  : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-white dark:border-slate-800 dark:bg-slate-950 dark:text-slate-200 dark:hover:border-slate-700 dark:hover:bg-slate-900'
              }`}
              onClick={() => void openConversation(item.sessionId)}
            >
              <div className="truncate text-sm font-semibold">{item.title || '未命名会话'}</div>
              <div className={`mt-1 line-clamp-2 text-xs leading-5 ${sessionId === item.sessionId ? 'text-slate-200 dark:text-slate-700' : 'text-slate-500 dark:text-slate-400'}`}>
                {item.lastMessagePreview || '暂无摘要'}
              </div>
            </button>
          ))}
        </div>
      </aside>

      <div className="flex min-h-0 flex-col gap-3 lg:gap-4">
        <section className="flex shrink-0 items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-4 dark:border-slate-800 dark:bg-slate-900 sm:px-5">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2 dark:border-slate-700 dark:bg-slate-950">
              <Bot className="h-5 w-5 text-slate-900 dark:text-slate-50" />
            </div>
            <div>
              <div className="text-base font-semibold text-slate-950 dark:text-slate-50">{activeTitle}</div>
              <div className="text-xs text-slate-500 dark:text-slate-300">{chatLoading ? '正在回复' : sessionId ? '会话中' : '新对话'}</div>
            </div>
          </div>
        </section>

        {error ? <div className="rounded-full bg-rose-50 px-4 py-2 text-center text-sm text-rose-600 dark:bg-rose-500/15 dark:text-rose-100">{error}</div> : null}
        {message ? <div className="rounded-full bg-emerald-50 px-4 py-2 text-center text-sm text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-100">{message}</div> : null}

        <div ref={listRef} className="min-h-0 flex-1 overflow-y-auto rounded-3xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-slate-950 sm:p-4">
          <div className="space-y-3">
            {messages.length ? messages.map(renderMessage) : <div className="py-20 text-center text-sm text-slate-400 dark:text-slate-500">开始对话</div>}
          </div>
        </div>

        <div className="shrink-0 space-y-3 rounded-2xl border border-slate-200 bg-white p-3 dark:border-slate-800 dark:bg-slate-900 sm:p-4">
          <div className="flex items-end gap-3">
            <textarea
              className="textarea min-h-[96px] flex-1"
              rows={4}
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="输入你的问题或操作需求，例如：帮我查 1-3-601 的未缴账单。"
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
