import { useEffect, useMemo, useState } from 'react'
import { Eye, RefreshCcw, Trash2, X } from 'lucide-react'

import { deleteBill, getAdminBills, getBillDetail } from '@/api/bills'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatDate, formatMoney, formatPeriod, formatServicePeriod } from '@/utils/format'
import type { BillDetail, BillListItem, BillListQuery } from '@/types/bill'

const initialQuery: BillListQuery = {
  pageNo: 1,
  pageSize: 10,
  periodYear: new Date().getFullYear(),
  periodMonth: undefined,
  feeType: '',
  status: '',
}

export default function BillsPage() {
  const [query, setQuery] = useState<BillListQuery>(initialQuery)
  const [list, setList] = useState<BillListItem[]>([])
  const [total, setTotal] = useState(0)
  const [detail, setDetail] = useState<BillDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function loadData(nextQuery = query) {
    setLoading(true)
    setError('')
    try {
      const result = await getAdminBills({
        ...nextQuery,
        feeType: nextQuery.feeType || undefined,
        status: nextQuery.status || undefined,
        periodMonth: nextQuery.periodMonth || undefined,
      })
      setList(result.list)
      setTotal(result.total)
    } catch (err) {
      setError(err instanceof Error ? err.message : '账单列表加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData(query)
  }, [query.pageNo, query.pageSize, query.periodYear, query.periodMonth, query.feeType, query.status])

  async function handleOpenDetail(billId: number) {
    setDetailLoading(true)
    setDetail(null)
    try {
      const result = await getBillDetail(billId)
      setDetail(result)
    } finally {
      setDetailLoading(false)
    }
  }

  async function handleDelete(item: BillListItem) {
    if (item.status !== 'ISSUED') {
      setError('仅未支付账单可删除。')
      return
    }
    if (!window.confirm(`确认删除账单 ${item.billNo} 吗？删除后将保留历史记录。`)) {
      return
    }
    setDeletingId(item.billId)
    setError('')
    setMessage('')
    try {
      await deleteBill(item.billId)
      setMessage('账单已作废。')
      if (detail?.billId === item.billId) {
        setDetail(null)
      }
      await loadData(query)
    } catch (err) {
      setError(err instanceof Error ? err.message : '作废账单失败')
    } finally {
      setDeletingId(null)
    }
  }

  const totalDue = useMemo(() => list.reduce((sum, item) => sum + Number(item.amountDue || 0), 0), [list])
  const totalPaid = useMemo(() => list.reduce((sum, item) => sum + Number(item.amountPaid || 0), 0), [list])

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">账单台账</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">账单列表</h1>
            </div>
            <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
              <RefreshCcw className="h-4 w-4" />
              {loading ? '刷新中...' : '刷新'}
            </button>
          </div>

          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            {[
              ['当前页应收', formatMoney(totalDue)],
              ['当前页已收', formatMoney(totalPaid)],
              ['账单数量', `${list.length} / ${total}`],
              ['当前账期', formatPeriod(query.periodYear, query.periodMonth || null, query.periodMonth ? 'MONTH' : 'YEAR')],
            ].map(([label, value]) => (
              <div key={label} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
                <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
              </div>
            ))}
          </div>
        </section>

        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection
          title="账单列表"
          description="按账期、费种与状态筛选。"
          action={
            <div className="flex flex-wrap gap-2">
              <input className="input w-28" type="number" min={2020} max={2100} value={query.periodYear || ''} onChange={(event) => setQuery((current) => ({ ...current, periodYear: Number(event.target.value) }))} />
              <select className="input w-24" value={query.periodMonth || ''} onChange={(event) => setQuery((current) => ({ ...current, periodMonth: event.target.value ? Number(event.target.value) : undefined }))}>
                <option value="">全年</option>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>{month} 月</option>
                ))}
              </select>
              <select className="input w-28" value={query.feeType} onChange={(event) => setQuery((current) => ({ ...current, feeType: event.target.value }))}>
                <option value="">全部费种</option>
                <option value="PROPERTY">物业费</option>
                <option value="WATER">水费</option>
              </select>
              <select className="input w-28" value={query.status} onChange={(event) => setQuery((current) => ({ ...current, status: event.target.value }))}>
                <option value="">全部状态</option>
                <option value="ISSUED">已出账</option>
                <option value="PAID">已支付</option>
                <option value="CANCELLED">已取消</option>
              </select>
            </div>
          }
        >
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前条件下暂无账单。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">账单编号</th>
                    <th className="px-4 py-3 font-medium">房间</th>
                    <th className="px-4 py-3 font-medium">费种</th>
                    <th className="px-4 py-3 font-medium">账期</th>
                    <th className="px-4 py-3 font-medium">应收</th>
                    <th className="px-4 py-3 font-medium">已收</th>
                    <th className="px-4 py-3 font-medium">到期日</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium text-right">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.billId} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.billNo}</td>
                      <td className="px-4 py-4 text-slate-600">{item.roomLabel}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.period}</td>
                      <td className="px-4 py-4 font-medium text-slate-900">{formatMoney(item.amountDue)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatMoney(item.amountPaid)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDate(item.dueDate)}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.status} /></td>
                      <td className="px-4 py-4">
                        <div className="flex justify-end gap-2">
                          <button type="button" className="btn-secondary min-h-10 gap-2 px-3 py-2" onClick={() => void handleOpenDetail(item.billId)}>
                            <Eye className="h-4 w-4" />
                            详情
                          </button>
                          {item.status === 'ISSUED' ? (
                            <button
                              type="button"
                              className="inline-flex min-h-10 cursor-pointer items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
                              onClick={() => void handleDelete(item)}
                              disabled={deletingId === item.billId}
                            >
                              <Trash2 className="h-4 w-4" />
                              {deletingId === item.billId ? '删除中' : '删除'}
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>
      </div>

      {(detail || detailLoading) ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">账单详情</div>
                <div className="mt-1 text-sm text-slate-500">查看服务周期与明细。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setDetail(null)}>
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4">
              <AsyncState loading={detailLoading} empty={!detail} emptyDescription="账单详情加载中。">
                {detail ? (
                  <div className="space-y-4">
                    <div className="panel-muted p-5">
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusBadge value={detail.feeType} />
                        <StatusBadge value={detail.cycleType} />
                        <StatusBadge value={detail.status} />
                      </div>
                      <div className="mt-4 text-xl font-semibold text-slate-950">{detail.billNo}</div>
                      <dl className="mt-4 space-y-3 text-sm">
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">房间</dt><dd className="font-medium text-slate-900">{detail.roomLabel}</dd></div>
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">户型</dt><dd className="font-medium text-slate-900">{detail.roomTypeName || '--'}</dd></div>
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">账期</dt><dd className="font-medium text-slate-900">{formatPeriod(detail.periodYear, detail.periodMonth || undefined, detail.cycleType)}</dd></div>
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">服务周期</dt><dd className="font-medium text-slate-900">{formatServicePeriod(detail.servicePeriodStart, detail.servicePeriodEnd)}</dd></div>
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">应收金额</dt><dd className="font-medium text-slate-900">{formatMoney(detail.amountDue)}</dd></div>
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">已收金额</dt><dd className="font-medium text-slate-900">{formatMoney(detail.amountPaid)}</dd></div>
                        <div className="flex items-center justify-between gap-3"><dt className="text-slate-500">到期日</dt><dd className="font-medium text-slate-900">{formatDate(detail.dueDate)}</dd></div>
                      </dl>
                    </div>

                    <div className="rounded-2xl border border-slate-200 bg-white p-5">
                      <div className="text-sm font-semibold text-slate-900">账单明细</div>
                      <div className="mt-4 space-y-3">
                        {detail.billLines.length ? detail.billLines.map((line) => (
                          <div key={`${line.lineNo}-${line.itemName}`} className="rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-4">
                            <div className="flex items-start justify-between gap-4">
                              <div>
                                <div className="font-medium text-slate-900">{line.itemName}</div>
                                <div className="mt-1 text-xs text-slate-500">条目类型：{line.lineType}</div>
                                {typeof line.ext?.prevReading === 'number' && typeof line.ext?.currReading === 'number' ? (
                                  <div className="mt-1 text-xs text-slate-500">
                                    上次读数 {line.ext.prevReading as number}，本次读数 {line.ext.currReading as number}，用量 {line.ext.usage as number | string}
                                  </div>
                                ) : null}
                                {line.ext?.pricingMode === 'TIERED' ? <div className="mt-1 text-xs text-slate-500">按阶梯水价计算</div> : null}
                              </div>
                              <div className="text-right">
                                <div className="font-semibold text-slate-900">{formatMoney(line.lineAmount)}</div>
                                <div className="mt-1 text-xs text-slate-500">
                                  {line.ext?.pricingMode === 'TIERED'
                                    ? '按阶梯水价结算'
                                    : `单价 ${formatMoney(line.unitPrice)} × 用量 ${line.quantity}`}
                                </div>
                              </div>
                            </div>
                          </div>
                        )) : <div className="text-sm text-slate-500">暂无账单条目。</div>}
                      </div>
                    </div>
                  </div>
                ) : null}
              </AsyncState>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
