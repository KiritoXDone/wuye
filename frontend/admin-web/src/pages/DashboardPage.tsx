import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Droplets, FileText, ReceiptText } from 'lucide-react'

import { getDashboardSummary, getPropertyYearlyReport, getWaterMonthlyReport } from '@/api/dashboard'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import { formatMoney, formatPercent, formatPeriod } from '@/utils/format'
import type { DashboardSummary, MonthlyReport } from '@/types/dashboard'

export default function DashboardPage() {
  const now = new Date()
  const [filters, setFilters] = useState({ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 })
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [monthly, setMonthly] = useState<MonthlyReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      const [summaryResult, yearlyResult, waterResult] = await Promise.all([
        getDashboardSummary(filters),
        getPropertyYearlyReport({ periodYear: filters.periodYear }),
        getWaterMonthlyReport(filters),
      ])
      setSummary(summaryResult)
      setMonthly({
        ...waterResult,
        paidAmount: yearlyResult.paidAmount,
        totalCount: summaryResult.totalCount,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : '仪表盘加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  const derived = useMemo(() => {
    const totalCount = Number(summary?.totalCount ?? 0)
    const paidCount = Number(summary?.paidCount ?? 0)
    const unpaidCount = Math.max(totalCount - paidCount, 0)
    const paidAmount = Number(summary?.paidAmount ?? 0)
    const unpaidAmount = Number(summary?.unpaidAmount ?? 0)
    const discountAmount = Number(summary?.discountAmount ?? 0)
    const payRate = Number(summary?.payRate ?? monthly?.payRate ?? 0)
    const averagePaidAmount = paidCount > 0 ? paidAmount / paidCount : 0
    return { totalCount, paidCount, unpaidCount, paidAmount, unpaidAmount, discountAmount, payRate, averagePaidAmount }
  }, [monthly?.payRate, summary])

  const heroCards = [
    { label: '账期实收', value: formatMoney(summary?.paidAmount), hint: '已回款' },
    { label: '欠费余额', value: formatMoney(summary?.unpaidAmount), hint: `${derived.unpaidCount} 间未缴` },
    { label: '收缴率', value: formatPercent(summary?.payRate), hint: `${derived.paidCount} / ${derived.totalCount}` },
    { label: '优惠影响', value: formatMoney(summary?.discountAmount), hint: '优惠金额' },
  ]

  const quickActions = [
    { to: '/billing-generate', label: '处理年度物业费开单', icon: ReceiptText },
    { to: '/water-readings', label: '录入月度水费抄表', icon: Droplets },
    { to: '/bills', label: '进入账单台账复核', icon: FileText },
  ]

  return (
    <div className="space-y-6 pb-2">
      <section className="space-y-5 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">经营总览</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">本期经营结果</h1>
          </div>

          <div className="flex flex-wrap gap-3">
            <label className="block min-w-[132px]">
              <span className="mb-2 block text-xs font-medium text-slate-500">账期年份</span>
              <input
                className="input"
                type="number"
                min={2020}
                max={2100}
                value={filters.periodYear}
                onChange={(event) => setFilters((current) => ({ ...current, periodYear: Number(event.target.value) }))}
              />
            </label>
            <label className="block min-w-[132px]">
              <span className="mb-2 block text-xs font-medium text-slate-500">账期月份</span>
              <select
                className="input"
                value={filters.periodMonth}
                onChange={(event) => setFilters((current) => ({ ...current, periodMonth: Number(event.target.value) }))}
              >
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>
                    {month} 月
                  </option>
                ))}
              </select>
            </label>
            <div className="flex items-end">
              <button type="button" className="btn-primary" onClick={() => void loadData()} disabled={loading}>
                {loading ? '刷新中...' : '更新'}
              </button>
            </div>
          </div>
        </div>

        <div className="grid gap-3 xl:grid-cols-4">
          {heroCards.map((card) => (
            <article key={card.label} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">{card.label}</div>
              <div className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{card.value}</div>
              <div className="mt-1 text-xs text-slate-500">{card.hint}</div>
            </article>
          ))}
        </div>
      </section>

      <AsyncState loading={loading} error={error} empty={!summary || !monthly}>
        <div className="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
          <PageSection title="经营复核" description="查看当期核心数据。">
            <div className="grid gap-4 lg:grid-cols-2">
              <div className="panel-muted p-5">
                <div className="text-sm font-medium text-slate-500">当前账期</div>
                <dl className="mt-4 space-y-4 text-sm">
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">水费账期</dt><dd className="font-semibold text-slate-900">{formatPeriod(monthly?.periodYear, monthly?.periodMonth, 'MONTH')}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">缴费率</dt><dd className="font-semibold text-slate-900">{formatPercent(monthly?.payRate)}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">年度物业费实收</dt><dd className="font-semibold text-slate-900">{formatMoney(monthly?.paidAmount)}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">当月水费优惠</dt><dd className="font-semibold text-slate-900">{formatMoney(monthly?.discountAmount)}</dd></div>
                  <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">当月水费欠费</dt><dd className="font-semibold text-slate-900">{formatMoney(monthly?.unpaidAmount)}</dd></div>
                </dl>
              </div>

              <div className="panel-muted p-5">
                <div className="text-sm font-medium text-slate-500">经营信号</div>
                <div className="mt-4 grid gap-3">
                  {[
                    ['未缴房间', `${derived.unpaidCount} 间`],
                    ['平均回款', formatMoney(derived.averagePaidAmount)],
                    ['总房间基数', `${derived.totalCount} 间`],
                  ].map(([label, value]) => (
                    <article key={label} className="rounded-xl border border-slate-200 bg-white px-4 py-4">
                      <div className="text-xs font-medium uppercase tracking-[0.14em] text-slate-500">{label}</div>
                      <div className="mt-2 text-xl font-semibold text-slate-950">{value}</div>
                    </article>
                  ))}
                </div>
              </div>
            </div>
          </PageSection>

          <PageSection title="快捷入口" description="进入高频操作。">
            <div className="space-y-4">
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <div className="text-sm font-medium text-slate-500">收缴进度</div>
                    <div className="mt-2 text-2xl font-semibold text-slate-950">{formatPercent(summary?.payRate)}</div>
                  </div>
                  <div className="tag">未缴 {derived.unpaidCount} 间</div>
                </div>
                <div className="mt-4 h-2 rounded-full bg-slate-200">
                  <div className="h-2 rounded-full bg-slate-900" style={{ width: `${Math.min(Math.max(derived.payRate, 0), 100)}%` }} />
                </div>
              </div>

              <div className="grid gap-3">
                {quickActions.map((action) => {
                  const Icon = action.icon
                  return (
                    <Link
                      key={action.to}
                      to={action.to}
                      className="group flex min-h-12 items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700 transition duration-150 hover:bg-slate-50 hover:text-slate-900"
                    >
                      <span className="flex items-center gap-3">
                        <Icon className="h-4 w-4" />
                        {action.label}
                      </span>
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  )
                })}
              </div>
            </div>
          </PageSection>
        </div>
      </AsyncState>
    </div>
  )
}
