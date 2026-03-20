import { useEffect, useState } from 'react'
import { RefreshCcw, Sparkles } from 'lucide-react'

import { getAiRuntimeConfig, updateAiRuntimeConfig } from '@/api/ai-runtime-config'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import type { AiRuntimeConfig, AiRuntimeConfigUpdatePayload } from '@/types/ai-runtime-config'

export default function AiRuntimeConfigPage() {
  const [current, setCurrent] = useState<AiRuntimeConfig | null>(null)
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<AiRuntimeConfigUpdatePayload>({
    enabled: false,
    apiBaseUrl: 'https://api.openai.com/v1',
    provider: 'openai',
    model: 'gpt-4o-mini',
    apiKey: '',
    timeoutMs: 30000,
    maxTokens: 4096,
    temperature: 0.2,
  })

  function patchForm(config: AiRuntimeConfig) {
    setForm({
      enabled: config.enabled,
      apiBaseUrl: config.apiBaseUrl,
      provider: config.provider,
      model: config.model,
      apiKey: '',
      timeoutMs: config.timeoutMs,
      maxTokens: config.maxTokens,
      temperature: config.temperature,
    })
  }

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      const result = await getAiRuntimeConfig()
      setCurrent(result)
      patchForm(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'AI 运行配置加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  async function handleSave() {
    if (!form.apiBaseUrl.trim() || !form.provider.trim() || !form.model.trim()) {
      setError('请填写 API 源、Provider 和模型名称。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await updateAiRuntimeConfig({
        ...form,
        apiKey: form.apiKey || undefined,
      })
      setCurrent(result)
      patchForm(result)
      setMessage('AI 运行配置保存成功。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存 AI 运行配置失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">系统配置</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">AI 运行配置</h1>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>
      </section>

      {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <PageSection title="当前配置" description="查看当前值。">
          <AsyncState loading={loading} error={error} empty={!current} emptyDescription="暂无 AI 运行配置。">
            {current ? (
              <div className="panel-muted p-5 text-sm">
                <div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><Sparkles className="h-4 w-4 text-primary-700" />当前配置快照</div>
                <dl className="mt-4 space-y-3">
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">启用状态</dt><dd className="font-medium text-slate-900">{current.enabled ? '已启用' : '未启用'}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">API 源</dt><dd className="max-w-[260px] truncate font-medium text-slate-900" title={current.apiBaseUrl}>{current.apiBaseUrl}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">Provider</dt><dd className="font-medium text-slate-900">{current.provider}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">模型</dt><dd className="font-medium text-slate-900">{current.model}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">已存密钥</dt><dd className="font-medium text-slate-900">{current.apiKeyMasked || '--'}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">超时（ms）</dt><dd className="font-medium text-slate-900">{current.timeoutMs}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">最大 Tokens</dt><dd className="font-medium text-slate-900">{current.maxTokens}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">Temperature</dt><dd className="font-medium text-slate-900">{current.temperature}</dd></div>
                </dl>
              </div>
            ) : null}
          </AsyncState>
        </PageSection>

        <PageSection title="更新配置" description="修改并保存。">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="block md:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">启用状态</span>
              <select className="input" value={form.enabled ? '1' : '0'} onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.value === '1' }))}>
                <option value="1">已启用</option>
                <option value="0">未启用</option>
              </select>
            </label>
            <label className="block md:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">API 源</span>
              <input className="input" value={form.apiBaseUrl} onChange={(event) => setForm((current) => ({ ...current, apiBaseUrl: event.target.value }))} placeholder="https://api.openai.com/v1" />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Provider</span>
              <input className="input" value={form.provider} onChange={(event) => setForm((current) => ({ ...current, provider: event.target.value }))} placeholder="openai / anthropic" />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">模型</span>
              <input className="input" value={form.model} onChange={(event) => setForm((current) => ({ ...current, model: event.target.value }))} placeholder="claude-sonnet-4-6" />
            </label>
            <label className="block md:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">新 API Key（可选）</span>
              <input className="input" type="password" value={form.apiKey || ''} onChange={(event) => setForm((current) => ({ ...current, apiKey: event.target.value }))} placeholder="不填则保留后端已存密钥" />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">超时（ms）</span>
              <input className="input" type="number" min={1000} max={120000} value={form.timeoutMs} onChange={(event) => setForm((current) => ({ ...current, timeoutMs: Number(event.target.value) }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">最大 Tokens</span>
              <input className="input" type="number" min={1} max={32768} value={form.maxTokens} onChange={(event) => setForm((current) => ({ ...current, maxTokens: Number(event.target.value) }))} />
            </label>
            <label className="block md:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">Temperature</span>
              <input className="input" type="number" min={0} max={2} step={0.1} value={form.temperature} onChange={(event) => setForm((current) => ({ ...current, temperature: Number(event.target.value) }))} />
            </label>
          </div>
          <button type="button" className="btn-primary mt-4 w-full" onClick={() => void handleSave()} disabled={submitLoading}>
            {submitLoading ? '保存中...' : '保存配置'}
          </button>
        </PageSection>
      </div>
    </div>
  )
}
