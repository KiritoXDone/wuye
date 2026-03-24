import { useDeferredValue, useEffect, useMemo, useState, type ReactNode } from 'react'
import { CheckCircle2, Droplets, Filter, Home, ReceiptText, RefreshCcw, Search, X } from 'lucide-react'

import { getCommunities } from '@/api/communities'
import { getHouseholdPaymentOverview, markBillPaid } from '@/api/bills'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { HouseholdPaymentOverviewItem, HouseholdPaymentOverviewQuery } from '@/types/bill'
import type { AdminCommunity } from '@/types/community'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'

const now = new Date()

const initialQuery: HouseholdPaymentOverviewQuery = {
  communityId: undefined,
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
  buildingNo: '',
  unitNo: '',
  roomKeyword: '',
  propertyStatus: '',
  waterStatus: '',
  pageNo: 1,
  pageSize: 12,
}

interface ManualMarkState {
  billId: number
  feeType: 'PROPERTY' | 'WATER'
  roomLabel: string
}

function normalizeQuery(query: HouseholdPaymentOverviewQuery, deferredRoomKeyword: string) {
  return {
    ...query,
    communityId: query.communityId || undefined,
    buildingNo: query.buildingNo || undefined,
    unitNo: query.unitNo || undefined,
    roomKeyword: deferredRoomKeyword || undefined,
    propertyStatus: query.propertyStatus || undefined,
    waterStatus: query.waterStatus || undefined,
  }
}

function FeeStatusCard({
  title,
  icon,
  status,
  amountDue,
  amountPaid,
  dueDate,
  paidAt,
  onMarkPaid,
}: {
  title: string
  icon: ReactNode
  status: string
  amountDue?: number | string | null
  amountPaid?: number | string | null
  dueDate?: string | null
  paidAt?: string | null
  onMarkPaid?: () => void
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-900 dark:text-slate-50">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">{icon}</span>
          {title}
        </div>
        <StatusBadge value={status} />
      </div>
      <dl className="mt-4 space-y-2 text-sm">
        <div className="flex items-center justify-between gap-3">
          <dt className="text-slate-500">应收</dt>
          <dd className="font-semibold text-slate-950 dark:text-slate-50">{amountDue == null ? '--' : formatMoney(amountDue)}</dd>
        </div>
        <div className="flex items-center justify-between gap-3">
          <dt className="text-slate-500">已收</dt>
          <dd className="font-medium text-slate-700 dark:text-slate-200">{amountPaid == null ? '--' : formatMoney(amountPaid)}</dd>
        </div>
        <div className="flex items-center justify-between gap-3">
          <dt className="text-slate-500">到期日</dt>
          <dd className="font-medium text-slate-900 dark:text-slate-100">{formatDate(dueDate)}</dd>
        </div>
        <div className="flex items-center justify-between gap-3">
          <dt className="text-slate-500">缴费时间</dt>
          <dd className="font-medium text-slate-900 dark:text-slate-100">{formatDateTime(paidAt)}</dd>
        </div>
      </dl>
      {status === 'ISSUED' && onMarkPaid ? (
        <button type="button" className="btn-secondary mt-4 w-full gap-2" onClick={onMarkPaid}>
          <CheckCircle2 className="h-4 w-4" />
          手动标记已缴
        </button>
      ) : null}
    </div>
  )
}

export default function HouseholdPaymentOverviewPage() {
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [query, setQuery] = useState<HouseholdPaymentOverviewQuery>(initialQuery)
  const [list, setList] = useState<HouseholdPaymentOverviewItem[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [communityLoading, setCommunityLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [manualMark, setManualMark] = useState<ManualMarkState | null>(null)
  const [markPaidAt, setMarkPaidAt] = useState('')
  const [markRemark, setMarkRemark] = useState('')
  const [markLoading, setMarkLoading] = useState(false)

  const deferredRoomKeyword = useDeferredValue(query.roomKeyword || '')

  async function loadCommunities() {
    setCommunityLoading(true)
    try {
      setCommunities(await getCommunities())
    } catch (err) {
      setError(err instanceof Error ? err.message : '小区列表加载失败')
    } finally {
      setCommunityLoading(false)
    }
  }

  async function loadData(nextQuery = query) {
    setLoading(true)
    setError('')
    try {
      const result = await getHouseholdPaymentOverview(normalizeQuery(nextQuery, deferredRoomKeyword))
      setList(result.list)
      setTotal(result.total)
    } catch (err) {
      setError(err instanceof Error ? err.message : '缴费统计加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadCommunities()
  }, [])

  useEffect(() => {
    void loadData(query)
  }, [
    query.communityId,
    query.periodYear,
    query.periodMonth,
    query.buildingNo,
    query.unitNo,
    query.propertyStatus,
    query.waterStatus,
    query.pageNo,
    query.pageSize,
    deferredRoomKeyword,
  ])

  const summary = useMemo(() => ({
    propertyPaid: list.filter((item) => item.propertyStatus === 'PAID').length,
    propertyOpen: list.filter((item) => item.propertyStatus === 'ISSUED').length,
    waterPaid: list.filter((item) => item.waterStatus === 'PAID').length,
    waterMissing: list.filter((item) => item.waterStatus === 'MISSING').length,
  }), [list])

  async function handleSubmitMark() {
    if (!manualMark) {
      return
    }
    setMarkLoading(true)
    setError('')
    setMessage('')
    try {
      await markBillPaid(manualMark.billId, {
        paidAt: markPaidAt || undefined,
        remark: markRemark || undefined,
      })
      setMessage(`${manualMark.roomLabel} 的${manualMark.feeType === 'PROPERTY' ? '物业费' : '水费'}已手动标记为已缴。`)
      setManualMark(null)
      setMarkPaidAt('')
      setMarkRemark('')
      await loadData(query)
    } catch (err) {
      setError(err instanceof Error ? err.message : '手动标记已缴失败')
    } finally {
      setMarkLoading(false)
    }
  }

  function openManualMark(item: HouseholdPaymentOverviewItem, feeType: 'PROPERTY' | 'WATER') {
    const billId = feeType === 'PROPERTY' ? item.propertyBillId : item.waterBillId
    if (!billId) {
      return
    }
    const nowValue = new Date()
    nowValue.setSeconds(0, 0)
    setManualMark({
      billId,
      feeType,
      roomLabel: item.roomLabel,
    })
    setMarkPaidAt(nowValue.toISOString().slice(0, 16))
    setMarkRemark('')
  }

  const totalPages = Math.max(1, Math.ceil(total / query.pageSize))

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="rounded-[24px] border border-slate-200 bg-white p-6 sm:p-7">
          <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
            <div className="max-w-3xl">
              <h1 className="text-3xl font-semibold tracking-tight text-slate-950">缴费统计总览</h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
                按房间查看指定年度物业费与指定月份水费的缴费状态。线下转账、现金收款和历史系统已缴场景，可在后台直接确认并保留审计记录。
              </p>
            </div>
            <button type="button" className="btn-secondary gap-2 self-start" onClick={() => void loadData()} disabled={loading}>
              <RefreshCcw className="h-4 w-4" />
              {loading ? '刷新中...' : '刷新数据'}
            </button>
          </div>

          <div className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            {[
              ['物业费已缴户', `${summary.propertyPaid}`],
              ['物业费待缴户', `${summary.propertyOpen}`],
              ['水费已缴户', `${summary.waterPaid}`],
              ['本月未出水费账单', `${summary.waterMissing}`],
            ].map(([label, value]) => (
              <div key={label} className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 dark:border-slate-800 dark:bg-slate-900">
                <div className="text-sm text-slate-500">{label}</div>
                <div className="mt-2 text-3xl font-semibold text-slate-950 dark:text-slate-50">{value}</div>
              </div>
            ))}
          </div>
        </section>

        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection
          title="筛选条件"
          description="默认按房间聚合查看指定年度物业费与指定月份水费。搜索会自动延迟执行，避免频繁请求。"
          action={
            <button
              type="button"
              className="btn-secondary gap-2"
              onClick={() => {
                setQuery(initialQuery)
                setMessage('')
                setError('')
              }}
            >
              <X className="h-4 w-4" />
              重置筛选
            </button>
          }
        >
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <label className="space-y-2 text-sm text-slate-600">
              <span>小区</span>
              <select
                className="input"
                value={query.communityId || ''}
                onChange={(event) => setQuery((current) => ({ ...current, communityId: event.target.value ? Number(event.target.value) : undefined, pageNo: 1 }))}
                disabled={communityLoading}
              >
                <option value="">全部小区</option>
                {communities.map((item) => (
                  <option key={item.id} value={item.id}>{item.name}</option>
                ))}
              </select>
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>物业费年份</span>
              <input className="input" type="number" min={2020} max={2100} value={query.periodYear} onChange={(event) => setQuery((current) => ({ ...current, periodYear: Number(event.target.value), pageNo: 1 }))} />
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>水费月份</span>
              <select className="input" value={query.periodMonth} onChange={(event) => setQuery((current) => ({ ...current, periodMonth: Number(event.target.value), pageNo: 1 }))}>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>{month} 月</option>
                ))}
              </select>
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>房号搜索</span>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input className="input pl-10" value={query.roomKeyword || ''} onChange={(event) => setQuery((current) => ({ ...current, roomKeyword: event.target.value, pageNo: 1 }))} placeholder="支持 1-2-301 / 301" />
              </div>
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>楼栋</span>
              <input className="input" value={query.buildingNo || ''} onChange={(event) => setQuery((current) => ({ ...current, buildingNo: event.target.value, pageNo: 1 }))} placeholder="如 1" />
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>单元</span>
              <input className="input" value={query.unitNo || ''} onChange={(event) => setQuery((current) => ({ ...current, unitNo: event.target.value, pageNo: 1 }))} placeholder="如 2" />
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>物业费状态</span>
              <select className="input" value={query.propertyStatus || ''} onChange={(event) => setQuery((current) => ({ ...current, propertyStatus: event.target.value, pageNo: 1 }))}>
                <option value="">全部</option>
                <option value="ISSUED">待缴</option>
                <option value="PAID">已缴</option>
                <option value="MISSING">未出账</option>
                <option value="CANCELLED">已作废</option>
              </select>
            </label>
            <label className="space-y-2 text-sm text-slate-600">
              <span>水费状态</span>
              <select className="input" value={query.waterStatus || ''} onChange={(event) => setQuery((current) => ({ ...current, waterStatus: event.target.value, pageNo: 1 }))}>
                <option value="">全部</option>
                <option value="ISSUED">待缴</option>
                <option value="PAID">已缴</option>
                <option value="MISSING">未出账</option>
                <option value="CANCELLED">已作废</option>
              </select>
            </label>
          </div>
        </PageSection>

        <PageSection
          title="逐户缴费状态"
          description={`当前共匹配 ${total} 户，按房间口径展示物业费与水费状态。`}
          action={
            <div className="tag gap-2">
              <Filter className="h-3.5 w-3.5" />
              第 {query.pageNo} / {totalPages} 页
            </div>
          }
        >
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前筛选条件下暂无房间记录。">
            <div className="grid gap-4 xl:grid-cols-2">
              {list.map((item) => (
                <article key={item.roomId} className="rounded-[24px] border border-slate-200 bg-white p-5">
                  <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-600"><Home className="h-4 w-4" /></span>
                        {item.roomLabel}
                      </div>
                      <div className="mt-2 text-sm text-slate-500">
                        {item.communityName}
                        {item.roomTypeName ? ` · ${item.roomTypeName}` : ''}
                        {item.areaM2 != null ? ` · ${Number(item.areaM2).toFixed(2)} m²` : ''}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <StatusBadge value={item.propertyStatus} />
                      <StatusBadge value={item.waterStatus} />
                    </div>
                  </div>

                  <div className="mt-4 grid gap-4 lg:grid-cols-2">
                    <FeeStatusCard
                      title={`${query.periodYear} 年物业费`}
                      icon={<ReceiptText className="h-4 w-4" />}
                      status={item.propertyStatus}
                      amountDue={item.propertyAmountDue}
                      amountPaid={item.propertyAmountPaid}
                      dueDate={item.propertyDueDate}
                      paidAt={item.propertyPaidAt}
                      onMarkPaid={item.propertyStatus === 'ISSUED' ? () => openManualMark(item, 'PROPERTY') : undefined}
                    />
                    <FeeStatusCard
                      title={`${query.periodYear} 年 ${String(query.periodMonth).padStart(2, '0')} 月水费`}
                      icon={<Droplets className="h-4 w-4" />}
                      status={item.waterStatus}
                      amountDue={item.waterAmountDue}
                      amountPaid={item.waterAmountPaid}
                      dueDate={item.waterDueDate}
                      paidAt={item.waterPaidAt}
                      onMarkPaid={item.waterStatus === 'ISSUED' ? () => openManualMark(item, 'WATER') : undefined}
                    />
                  </div>
                </article>
              ))}
            </div>

            <div className="mt-6 flex flex-col gap-3 border-t border-slate-200 pt-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="text-sm text-slate-500">每页展示 {query.pageSize} 户，当前共 {total} 户。</div>
              <div className="flex gap-2">
                <button type="button" className="btn-secondary" disabled={query.pageNo <= 1} onClick={() => setQuery((current) => ({ ...current, pageNo: current.pageNo - 1 }))}>上一页</button>
                <button type="button" className="btn-secondary" disabled={query.pageNo >= totalPages} onClick={() => setQuery((current) => ({ ...current, pageNo: current.pageNo + 1 }))}>下一页</button>
              </div>
            </div>
          </AsyncState>
        </PageSection>
      </div>

      {manualMark ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-[24px] border border-slate-200 bg-white p-6 shadow-xl shadow-slate-900/5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">手动标记已缴</div>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  {manualMark.roomLabel} 的{manualMark.feeType === 'PROPERTY' ? '物业费' : '水费'}将按线下已收处理，并写入审计日志。
                </p>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setManualMark(null)}>
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-5 space-y-4">
              <label className="space-y-2 text-sm text-slate-600">
                <span>缴费时间</span>
                <input className="input" type="datetime-local" value={markPaidAt} onChange={(event) => setMarkPaidAt(event.target.value)} />
              </label>
              <label className="space-y-2 text-sm text-slate-600">
                <span>备注</span>
                <textarea className="textarea min-h-28" value={markRemark} onChange={(event) => setMarkRemark(event.target.value)} placeholder="如：线下转账已核验、现金收款、历史系统已缴等" />
              </label>
            </div>

            <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <button type="button" className="btn-secondary" onClick={() => setManualMark(null)} disabled={markLoading}>取消</button>
              <button type="button" className="btn-primary gap-2" onClick={() => void handleSubmitMark()} disabled={markLoading}>
                <CheckCircle2 className="h-4 w-4" />
                {markLoading ? '提交中...' : '确认标记已缴'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
