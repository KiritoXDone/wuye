import { useEffect, useState } from 'react'
import { Bot, RefreshCcw } from 'lucide-react'

import { getAdminAgentBillStats } from '@/api/agent'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import { formatMoney, formatPercent } from '@/utils/format'
import type { AgentAdminBillStats } from '@/types/agent'

export default function BuiltInAgentPage() {
  const now = new Date()
  const [filters, setFilters] = useState({ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 })
  const [result, setResult] = useState<AgentAdminBillStats | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      setResult(await getAdminAgentBillStats(filters))
    } catch (err) {
      setError(err instanceof Error ? err.message : '内置 agent 统计加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">系统能力</div>
            <h1 className="mt-2 flex items-center gap-2 text-2xl font-semibold text-slate-950"><Bot className="h-6 w-6" />内置 Agent</h1>
            <p className="mt-2 text-sm text-slate-500">提供受限账单统计能力，不作为独立登录角色。</p>
          </div>
          <div className="flex flex-wrap gap-3">
            <input className="input w-32" type="number" value={filters.periodYear} onChange={(event) => setFilters((current) => ({ ...current, periodYear: Number(event.target.value) }))} />
            <select className="input w-32" value={filters.periodMonth} onChange={(event) => setFilters((current) => ({ ...current, periodMonth: Number(event.target.value) }))}>
              {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                <option key={month} value={month}>{month} 月</option>
              ))}
            </select>
            <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
              <RefreshCcw className="h-4 w-4" />
              {loading ? '刷新中...' : '刷新'}
            </button>
          </div>
        </div>
      </section>

      <AsyncState loading={loading} error={error} empty={!result} emptyDescription="暂无统计结果。">
        {result ? (
          <div className="grid gap-6">
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
              {[
                ['账期实收', formatMoney(result.summary.paidAmount)],
                ['欠费余额', formatMoney(result.summary.unpaidAmount)],
                ['收缴率', formatPercent(result.summary.payRate)],
                ['优惠金额', formatMoney(result.summary.discountAmount)],
              ].map(([label, value]) => (
                <div key={label} className="rounded-xl border border-slate-200 bg-white p-4">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
                  <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
                </div>
              ))}
            </div>

            <div className="grid gap-6 xl:grid-cols-2">
              <PageSection title="物业费年度汇总" description="内置 agent 返回的年度物业费统计。">
                <dl className="grid gap-3 text-sm">
                  <div className="flex items-center justify-between"><dt className="text-slate-500">年度</dt><dd className="font-medium text-slate-900">{result.propertyYearly.periodYear}</dd></div>
                  <div className="flex items-center justify-between"><dt className="text-slate-500">已缴房间</dt><dd className="font-medium text-slate-900">{result.propertyYearly.paidCount}</dd></div>
                  <div className="flex items-center justify-between"><dt className="text-slate-500">总房间数</dt><dd className="font-medium text-slate-900">{result.propertyYearly.totalCount}</dd></div>
                  <div className="flex items-center justify-between"><dt className="text-slate-500">实收金额</dt><dd className="font-medium text-slate-900">{formatMoney(result.propertyYearly.paidAmount)}</dd></div>
                </dl>
              </PageSection>

              <PageSection title="水费月度汇总" description="内置 agent 返回的月度水费统计。">
                <dl className="grid gap-3 text-sm">
                  <div className="flex items-center justify-between"><dt className="text-slate-500">账期</dt><dd className="font-medium text-slate-900">{result.waterMonthly.periodYear}-{String(result.waterMonthly.periodMonth).padStart(2, '0')}</dd></div>
                  <div className="flex items-center justify-between"><dt className="text-slate-500">缴费率</dt><dd className="font-medium text-slate-900">{formatPercent(result.waterMonthly.payRate)}</dd></div>
                  <div className="flex items-center justify-between"><dt className="text-slate-500">优惠金额</dt><dd className="font-medium text-slate-900">{formatMoney(result.waterMonthly.discountAmount)}</dd></div>
                  <div className="flex items-center justify-between"><dt className="text-slate-500">欠费金额</dt><dd className="font-medium text-slate-900">{formatMoney(result.waterMonthly.unpaidAmount)}</dd></div>
                </dl>
              </PageSection>
            </div>
          </div>
        ) : null}
      </AsyncState>
    </div>
  )
}
