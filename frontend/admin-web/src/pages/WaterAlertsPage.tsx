import { useEffect, useState } from 'react'
import { AlertTriangle, RefreshCcw } from 'lucide-react'

import { getWaterAlerts } from '@/api/water-alerts'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { WaterAlert } from '@/types/water-alert'
import { formatDateTime, formatQuantity, formatPeriod } from '@/utils/format'

const now = new Date()

export default function WaterAlertsPage() {
  const [filters, setFilters] = useState({ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 })
  const [list, setList] = useState<WaterAlert[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function loadData(next = filters) {
    setLoading(true)
    setError('')
    try {
      setList(await getWaterAlerts(next))
    } catch (err) {
      setError(err instanceof Error ? err.message : '水量预警加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData(filters)
  }, [])

  return (
    <div className="space-y-6 pb-2">
      <section className="glass-panel overflow-hidden p-6 sm:p-7">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">水费风险提示</div>
            <h1 className="mt-3 text-3xl font-semibold text-slate-950">按账期查看异常水量预警，辅助追踪但不默认阻断开单</h1>
            <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
              当前后端主要返回 OPEN 状态预警事件，用于识别绝对阈值或倍数阈值触发的异常房间，帮助运营在月度抄表闭环中及时复核。
            </p>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新预警'}
          </button>
        </div>
      </section>

      <PageSection
        title="预警列表"
        description="阈值与实际值统一保留三位小数，便于快速核对异常区间。"
        action={
          <div className="flex flex-wrap gap-2">
            <input className="input w-28" type="number" min={2020} max={2100} value={filters.periodYear} onChange={(event) => setFilters((current) => ({ ...current, periodYear: Number(event.target.value) }))} />
            <select className="input w-24" value={filters.periodMonth} onChange={(event) => setFilters((current) => ({ ...current, periodMonth: Number(event.target.value) }))}>
              {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                <option key={month} value={month}>{month} 月</option>
              ))}
            </select>
            <button type="button" className="btn-primary" onClick={() => void loadData(filters)} disabled={loading}>查询预警</button>
          </div>
        }
      >
        <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前账期暂无水量预警。">
          <div className="mb-4 flex items-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            <AlertTriangle className="h-4 w-4" />
            当前账期：{formatPeriod(filters.periodYear, filters.periodMonth, 'MONTH')}。
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-4 py-3 font-medium">房间</th>
                  <th className="px-4 py-3 font-medium">抄表 ID</th>
                  <th className="px-4 py-3 font-medium">预警类型</th>
                  <th className="px-4 py-3 font-medium">预警说明</th>
                  <th className="px-4 py-3 font-medium">阈值</th>
                  <th className="px-4 py-3 font-medium">实际值</th>
                  <th className="px-4 py-3 font-medium">状态</th>
                  <th className="px-4 py-3 font-medium">创建时间</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="px-4 py-4 font-medium text-slate-900">{item.roomLabel}</td>
                    <td className="px-4 py-4 text-slate-600">{item.readingId}</td>
                    <td className="px-4 py-4"><StatusBadge value={item.alertCode} /></td>
                    <td className="px-4 py-4 text-slate-600">{item.alertMessage}</td>
                    <td className="px-4 py-4 text-right text-slate-600">{formatQuantity(item.thresholdValue)}</td>
                    <td className="px-4 py-4 text-right font-medium text-slate-900">{formatQuantity(item.actualValue)}</td>
                    <td className="px-4 py-4"><StatusBadge value={item.status} /></td>
                    <td className="px-4 py-4 text-slate-600">{formatDateTime(item.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </AsyncState>
      </PageSection>
    </div>
  )
}
