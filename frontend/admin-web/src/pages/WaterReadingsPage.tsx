import { useEffect, useMemo, useState } from 'react'
import { Droplets, RefreshCcw } from 'lucide-react'

import { createWaterReading, getWaterReadings } from '@/api/water'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatDateTime, formatPeriod, formatQuantity } from '@/utils/format'
import type { WaterReading } from '@/types/water'

const now = new Date()

export default function WaterReadingsPage() {
  const [filters, setFilters] = useState({ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 })
  const [form, setForm] = useState({
    roomId: 101,
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    prevReading: 120,
    currReading: 135,
    readAt: new Date().toISOString().slice(0, 16),
    remark: '',
  })
  const [list, setList] = useState<WaterReading[]>([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [submitResult, setSubmitResult] = useState('')

  const usagePreview = useMemo(() => Math.max(Number(form.currReading) - Number(form.prevReading), 0), [form.currReading, form.prevReading])

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      const result = await getWaterReadings(filters)
      setList(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '抄表记录加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setSubmitError('')
    setSubmitResult('')

    if (Number(form.currReading) < Number(form.prevReading)) {
      setSubmitError('本次读数不能小于上次读数。')
      setSubmitting(false)
      return
    }

    try {
      const result = await createWaterReading({
        roomId: Number(form.roomId),
        year: Number(form.year),
        month: Number(form.month),
        prevReading: Number(form.prevReading),
        currReading: Number(form.currReading),
        readAt: new Date(form.readAt).toISOString(),
        remark: form.remark || undefined,
      })
      setSubmitResult(`房间 ${form.roomId} 的 ${formatPeriod(form.year, form.month, 'MONTH')} 抄表已录入，并已生成水费账单 ${result.billNo}。`)
      await loadData()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : '抄表录入失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">水费抄表</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">录入抄表</h1>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
        <PageSection title="录入抄表" description="录入后立即出账。">
          <form className="grid gap-4" onSubmit={handleSubmit}>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">房间 ID</span>
                <input className="input" type="number" min={1} value={form.roomId} onChange={(event) => setForm((current) => ({ ...current, roomId: Number(event.target.value) }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">年份</span>
                <input className="input" type="number" min={2020} max={2100} value={form.year} onChange={(event) => setForm((current) => ({ ...current, year: Number(event.target.value) }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">月份</span>
                <select className="input" value={form.month} onChange={(event) => setForm((current) => ({ ...current, month: Number(event.target.value) }))}>
                  {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                    <option key={month} value={month}>{month} 月</option>
                  ))}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">抄表时间</span>
                <input className="input" type="datetime-local" value={form.readAt} onChange={(event) => setForm((current) => ({ ...current, readAt: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">上次读数</span>
                <input className="input" type="number" step="0.001" value={form.prevReading} onChange={(event) => setForm((current) => ({ ...current, prevReading: Number(event.target.value) }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">本次读数</span>
                <input className="input" type="number" step="0.001" value={form.currReading} onChange={(event) => setForm((current) => ({ ...current, currReading: Number(event.target.value) }))} />
              </label>
            </div>

            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">备注</span>
              <textarea className="textarea" rows={4} value={form.remark} onChange={(event) => setForm((current) => ({ ...current, remark: event.target.value }))} placeholder="可填写现场异常说明、表具状态等。" />
            </label>

            <div className="panel-muted p-5">
              <div className="text-sm font-medium text-slate-500">用量预览</div>
              <div className="mt-2 text-3xl font-semibold text-slate-950">{formatQuantity(usagePreview)}</div>
            </div>

            {submitError ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{submitError}</div> : null}
            {submitResult ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{submitResult}</div> : null}

            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? '提交中...' : '录入抄表并触发出账'}
            </button>
          </form>
        </PageSection>

        <PageSection
          title="当期抄表记录"
          description="按账期查看。"
          action={
            <div className="flex flex-wrap gap-2">
              <input className="input w-28" type="number" min={2020} max={2100} value={filters.periodYear} onChange={(event) => setFilters((current) => ({ ...current, periodYear: Number(event.target.value) }))} />
              <select className="input w-24" value={filters.periodMonth} onChange={(event) => setFilters((current) => ({ ...current, periodMonth: Number(event.target.value) }))}>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>{month} 月</option>
                ))}
              </select>
              <button type="button" className="btn-secondary" onClick={() => void loadData()} disabled={loading}>查询</button>
            </div>
          }
        >
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前账期暂无抄表记录。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">房间</th>
                    <th className="px-4 py-3 font-medium">账期</th>
                    <th className="px-4 py-3 font-medium">上次读数</th>
                    <th className="px-4 py-3 font-medium">本次读数</th>
                    <th className="px-4 py-3 font-medium">用量</th>
                    <th className="px-4 py-3 font-medium">抄表时间</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.roomLabel}</td>
                      <td className="px-4 py-4 text-slate-600">{formatPeriod(item.periodYear, item.periodMonth)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatQuantity(item.prevReading)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatQuantity(item.currReading)}</td>
                      <td className="px-4 py-4 text-slate-900">{formatQuantity(item.usageAmount)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDateTime(item.readAt)}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>
      </div>
    </div>
  )
}
