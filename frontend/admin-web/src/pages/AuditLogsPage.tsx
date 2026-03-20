import { useEffect, useMemo, useState } from 'react'
import { Search, ShieldCheck } from 'lucide-react'

import { getAuditLogs } from '@/api/audit-logs'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { AuditLogItem, AuditLogListQuery } from '@/types/audit-log'
import { formatDateTime } from '@/utils/format'

const bizTypeOptions = [
  { label: '全部业务', value: '' },
  { label: '账单', value: 'BILL' },
  { label: '支付', value: 'PAYMENT' },
  { label: '券', value: 'COUPON' },
  { label: '登录鉴权', value: 'AUTH' },
  { label: '导入', value: 'IMPORT' },
  { label: '导出', value: 'EXPORT' },
]

export default function AuditLogsPage() {
  const [filters, setFilters] = useState<AuditLogListQuery>({ pageNo: 1, pageSize: 10, bizType: '', bizId: '', operatorId: undefined, createdAtStart: undefined, createdAtEnd: undefined })
  const [list, setList] = useState<AuditLogItem[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selectedLog, setSelectedLog] = useState<AuditLogItem | null>(null)

  async function loadData(next = filters) {
    setLoading(true)
    setError('')
    try {
      const result = await getAuditLogs(next)
      setList(result.list)
      setTotal(result.total)
    } catch (err) {
      setError(err instanceof Error ? err.message : '审计日志加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData(filters)
  }, [])

  const formattedDetailJson = useMemo(() => {
    const detailJson = selectedLog?.detailJson
    if (!detailJson) {
      return '--'
    }
    try {
      return JSON.stringify(JSON.parse(detailJson), null, 2)
    } catch {
      return detailJson
    }
  }, [selectedLog])

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div>
          <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">治理审计</div>
          <h1 className="mt-2 text-2xl font-semibold text-slate-950">审计日志</h1>
        </div>
      </section>

      <PageSection title="筛选条件" description="筛选并查询。">
        <div className="flex flex-wrap gap-3">
          <select className="input w-40" value={filters.bizType || ''} onChange={(event) => setFilters((current) => ({ ...current, bizType: event.target.value, pageNo: 1 }))}>
            {bizTypeOptions.map((option) => <option key={option.value || 'ALL'} value={option.value}>{option.label}</option>)}
          </select>
          <input className="input w-44" value={filters.bizId || ''} onChange={(event) => setFilters((current) => ({ ...current, bizId: event.target.value, pageNo: 1 }))} placeholder="业务主键" />
          <input className="input w-36" type="number" min={1} value={filters.operatorId || ''} onChange={(event) => setFilters((current) => ({ ...current, operatorId: event.target.value ? Number(event.target.value) : undefined, pageNo: 1 }))} placeholder="操作人 ID" />
          <input className="input w-48" type="datetime-local" value={filters.createdAtStart ? filters.createdAtStart.replace(' ', 'T') : ''} onChange={(event) => setFilters((current) => ({ ...current, createdAtStart: event.target.value ? event.target.value.replace('T', ' ') + ':00' : undefined, pageNo: 1 }))} />
          <input className="input w-48" type="datetime-local" value={filters.createdAtEnd ? filters.createdAtEnd.replace(' ', 'T') : ''} onChange={(event) => setFilters((current) => ({ ...current, createdAtEnd: event.target.value ? event.target.value.replace('T', ' ') + ':00' : undefined, pageNo: 1 }))} />
          <button type="button" className="btn-primary gap-2" onClick={() => void loadData(filters)} disabled={loading}>
            <Search className="h-4 w-4" />
            查询
          </button>
          <button type="button" className="btn-secondary" onClick={() => { const reset = { pageNo: 1, pageSize: filters.pageSize, bizType: '', bizId: '', operatorId: undefined, createdAtStart: undefined, createdAtEnd: undefined }; setFilters(reset); void loadData(reset) }}>重置</button>
        </div>
      </PageSection>

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.9fr]">
        <PageSection title="日志列表" description="列表聚焦最关键的定位字段，详情统一放到右侧信息面板。">
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前条件下暂无审计日志。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">审计 ID</th>
                    <th className="px-4 py-3 font-medium">业务类型</th>
                    <th className="px-4 py-3 font-medium">业务主键</th>
                    <th className="px-4 py-3 font-medium">操作动作</th>
                    <th className="px-4 py-3 font-medium">操作人 ID</th>
                    <th className="px-4 py-3 font-medium">来源 IP</th>
                    <th className="px-4 py-3 font-medium">创建时间</th>
                    <th className="px-4 py-3 font-medium">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.id}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.bizType} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.bizId}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.action} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.operatorId ?? '--'}</td>
                      <td className="px-4 py-4 text-slate-600">{item.ip || '--'}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDateTime(item.createdAt)}</td>
                      <td className="px-4 py-4"><button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setSelectedLog(item)}>查看明细</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
              <span>共 {total} 条，当前第 {filters.pageNo} 页</span>
              <div className="flex gap-2">
                <button type="button" className="btn-secondary px-3 py-2" disabled={filters.pageNo <= 1 || loading} onClick={() => { const next = { ...filters, pageNo: filters.pageNo - 1 }; setFilters(next); void loadData(next) }}>上一页</button>
                <button type="button" className="btn-secondary px-3 py-2" disabled={filters.pageNo * filters.pageSize >= total || loading} onClick={() => { const next = { ...filters, pageNo: filters.pageNo + 1 }; setFilters(next); void loadData(next) }}>下一页</button>
              </div>
            </div>
          </AsyncState>
        </PageSection>

        <PageSection title="审计明细" description="展示 detailJson 快照，便于定位业务上下文。">
          <AsyncState empty={!selectedLog} emptyDescription="请先从左侧选择一条审计日志。">
            {selectedLog ? (
              <div className="space-y-4">
                <div className="glass-soft rounded-[24px] p-5 text-sm">
                  <div className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-primary-700" /><span className="font-semibold text-slate-900">基础信息</span></div>
                  <dl className="mt-4 space-y-3">
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">审计 ID</dt><dd className="font-medium text-slate-900">{selectedLog.id}</dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">业务类型</dt><dd><StatusBadge value={selectedLog.bizType} /></dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">业务主键</dt><dd className="font-medium text-slate-900">{selectedLog.bizId}</dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">操作动作</dt><dd><StatusBadge value={selectedLog.action} /></dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">操作人</dt><dd className="font-medium text-slate-900">{selectedLog.operatorId ?? '--'}</dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">来源 IP</dt><dd className="font-medium text-slate-900">{selectedLog.ip || '--'}</dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">User-Agent</dt><dd className="max-w-[260px] truncate font-medium text-slate-900" title={selectedLog.userAgent || '--'}>{selectedLog.userAgent || '--'}</dd></div>
                    <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">创建时间</dt><dd className="font-medium text-slate-900">{formatDateTime(selectedLog.createdAt)}</dd></div>
                  </dl>
                </div>
                <div className="rounded-[24px] border border-slate-200/80 bg-white/75 p-5 backdrop-blur-xl">
                  <div className="text-sm font-semibold text-slate-900">detailJson</div>
                  <pre className="mt-4 overflow-x-auto rounded-2xl bg-slate-950/95 p-4 text-xs leading-6 text-slate-100">{formattedDetailJson}</pre>
                </div>
              </div>
            ) : null}
          </AsyncState>
        </PageSection>
      </div>
    </div>
  )
}
