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
    { label: '账期实收', value: formatMoney(summary?.paidAmount), hint: '用于核对本账期已完成回款规模' },
    { label: '欠费余额', value: formatMoney(summary?.unpaidAmount), hint: `${derived.unpaidCount} 个房间待持续跟进` },
    { label: '收缴率', value: formatPercent(summary?.payRate), hint: `已缴 ${derived.paidCount} / 房间总数 ${derived.totalCount}` },
    { label: '优惠影响', value: formatMoney(summary?.discountAmount), hint: '用于核对优惠策略对实收的影响' },
  ]

  const quickActions = [
    { to: '/billing-generate', label: '处理年度物业费开单', icon: ReceiptText },
    { to: '/water-readings', label: '录入月度水费抄表', icon: Droplets },
    { to: '/bills', label: '进入账单台账复核', icon: FileText },
  ]

  return (
    <div className="space-y-6 pb-2">
      <section className="panel overflow-hidden p-6 sm:p-7">
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">经营总览</div>
              <h1 className="mt-3 text-3xl font-semibold text-slate-950">围绕房间口径查看本期经营结果</h1>
              <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-500">
                将实收、欠费、优惠和收缴率统一放在同一视图，帮助收费运营与财务用同一套经营数字做判断。
              </p>
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
                  {loading ? '刷新中...' : '更新视图'}
                </button>
              </div>
            </div>
          </div>

          <div className="grid gap-4 xl:grid-cols-4">
            {heroCards.map((card) => (
              <article key={card.label} className="rounded-[24px] border border-slate-200 bg-white/80 p-5 backdrop-blur">
                <div className="text-sm text-slate-500">{card.label}</div>
                <div className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">{card.value}</div>
                <div className="mt-2 text-sm leading-6 text-slate-500">{card.hint}</div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <AsyncState loading={loading} error={error} empty={!summary || !monthly}>
        <div className="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
          <PageSection title="经营复核" description="先确认本月经营结果，再进入账单和抄表页面处理具体动作。">
            <div className="grid gap-4 lg:grid-cols-2">
              <div className="panel-muted p-5">
                <div className="text-sm font-medium text-slate-500">当前账期摘要</div>
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
                    ['未缴房间', `${derived.unpaidCount} 间`, '建议联动账单台账逐笔跟进'],
                    ['平均回款', formatMoney(derived.averagePaidAmount), '按本月已缴房间均值观察回款表现'],
                    ['总房间基数', `${derived.totalCount} 间`, '经营统计统一按房间口径汇总'],
                  ].map(([label, value, hint]) => (
                    <article key={label} className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                      <div className="text-xs font-medium uppercase tracking-[0.14em] text-slate-500">{label}</div>
                      <div className="mt-2 text-xl font-semibold text-slate-950">{value}</div>
                      <div className="mt-1 text-sm leading-6 text-slate-500">{hint}</div>
                    </article>
                  ))}
                </div>
              </div>
            </div>
          </PageSection>

          <PageSection title="执行重点" description="按运营日常节奏排列动作，减少首页后的决策跳转。">
            <div className="space-y-4">
              <div className="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <div className="text-sm font-medium text-slate-500">收缴进度</div>
                    <div className="mt-2 text-2xl font-semibold text-slate-950">{formatPercent(summary?.payRate)}</div>
                  </div>
                  <div className="tag">未缴 {derived.unpaidCount} 间</div>
                </div>
                <div className="mt-4 h-3 rounded-full bg-slate-200">
                  <div className="h-3 rounded-full bg-primary-600" style={{ width: `${Math.min(Math.max(derived.payRate, 0), 100)}%` }} />
                </div>
              </div>

              <div className="space-y-3">
                {[
                  `先确认 ${formatPeriod(filters.periodYear, filters.periodMonth)} 的实收、欠费与优惠金额是否一致。`,
                  `再锁定 ${derived.unpaidCount} 个未缴房间，进入账单台账逐笔复核。`,
                  '最后回到开单与抄表入口，检查是否存在规则或录入偏差。',
                ].map((text, index) => (
                  <div key={text} className="rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm leading-7 text-slate-600">
                    <span className="mr-3 inline-flex h-7 w-7 items-center justify-center rounded-full bg-primary-50 text-xs font-semibold text-primary-700">{index + 1}</span>
                    {text}
                  </div>
                ))}
              </div>

              <div className="grid gap-3">
                {quickActions.map((action) => {
                  const Icon = action.icon
                  return (
                    <Link
                      key={action.to}
                      to={action.to}
                      className="group flex min-h-14 items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-primary-200 hover:bg-primary-50 hover:text-primary-800"
                    >
                      <span className="flex items-center gap-3">
                        <span className="rounded-xl bg-slate-100 p-2 group-hover:bg-white">
                          <Icon className="h-4 w-4" />
                        </span>
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
