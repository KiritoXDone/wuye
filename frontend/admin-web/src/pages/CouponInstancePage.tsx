import { useEffect, useMemo, useState } from 'react'
import { Gift, RefreshCcw, Search, X } from 'lucide-react'

import { getCouponInstances, getCouponTemplates, manualIssueCoupon } from '@/api/coupons'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { CouponInstance, CouponInstanceQuery, CouponManualIssuePayload, CouponTemplate } from '@/types/coupon'
import { formatDateTime } from '@/utils/format'

const initialFilters: CouponInstanceQuery = {
  templateKeyword: '',
  status: '',
  sourceType: '',
  ownerAccountId: undefined,
}

const initialForm: CouponManualIssuePayload = {
  templateId: 0,
  ownerAccountId: 0,
  issueCount: 1,
  remark: '',
}

export default function CouponInstancePage() {
  const [list, setList] = useState<CouponInstance[]>([])
  const [templates, setTemplates] = useState<CouponTemplate[]>([])
  const [filters, setFilters] = useState<CouponInstanceQuery>(initialFilters)
  const [form, setForm] = useState<CouponManualIssuePayload>(initialForm)
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [showIssueModal, setShowIssueModal] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const activeTemplates = useMemo(() => templates.filter((item) => item.status === 1), [templates])

  async function loadData(nextFilters = filters) {
    setLoading(true)
    setError('')
    try {
      const [instanceList, templateList] = await Promise.all([
        getCouponInstances(nextFilters),
        getCouponTemplates(),
      ])
      setList(instanceList)
      setTemplates(templateList)
    } catch (err) {
      setError(err instanceof Error ? err.message : '券实例加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  function handleSearch() {
    void loadData(filters)
  }

  async function handleManualIssue() {
    if (!form.templateId || !form.ownerAccountId || !form.issueCount) {
      setError('请填写模板、账号 ID 和发放张数。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await manualIssueCoupon({
        templateId: Number(form.templateId),
        ownerAccountId: Number(form.ownerAccountId),
        issueCount: Number(form.issueCount),
        remark: form.remark?.trim() || undefined,
      })
      setMessage(`手工发券成功，共发放 ${result.issueCount} 张。`)
      setShowIssueModal(false)
      setForm(initialForm)
      await loadData(filters)
    } catch (err) {
      setError(err instanceof Error ? err.message : '手工发券失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">券体系</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">券实例</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">查看券发放结果，并支持最小范围的手工补发。</p>
            </div>
            <div className="flex flex-wrap gap-2 xl:justify-end">
              <button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={() => void loadData(filters)} disabled={loading}>
                <RefreshCcw className="h-4 w-4" />
                {loading ? '刷新中...' : '刷新'}
              </button>
              <button type="button" className="btn-primary gap-2 whitespace-nowrap" onClick={() => setShowIssueModal(true)}>
                <Gift className="h-4 w-4" />
                手工发券
              </button>
            </div>
          </div>
        </section>

        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection
          title="实例列表"
          description="按模板关键字、状态、来源和账号筛选。"
          action={<button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={handleSearch} disabled={loading}><Search className="h-4 w-4" />查询</button>}
        >
          <div className="mb-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">模板关键字</span>
              <input className="input" value={filters.templateKeyword || ''} onChange={(event) => setFilters((current) => ({ ...current, templateKeyword: event.target.value }))} placeholder="模板编码或名称" />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">实例状态</span>
              <select className="input" value={filters.status || ''} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value || undefined }))}>
                <option value="">全部状态</option>
                <option value="NEW">NEW</option>
                <option value="LOCKED">LOCKED</option>
                <option value="USED">USED</option>
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">来源类型</span>
              <select className="input" value={filters.sourceType || ''} onChange={(event) => setFilters((current) => ({ ...current, sourceType: event.target.value || undefined }))}>
                <option value="">全部来源</option>
                <option value="MANUAL">MANUAL</option>
                <option value="PAYMENT_REWARD">PAYMENT_REWARD</option>
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">账号 ID</span>
              <input className="input" type="number" min={1} value={filters.ownerAccountId || ''} onChange={(event) => setFilters((current) => ({ ...current, ownerAccountId: event.target.value ? Number(event.target.value) : undefined }))} placeholder="例如 1001" />
            </label>
          </div>

          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前条件下暂无券实例。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="whitespace-nowrap px-4 py-3 font-medium">实例 ID</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">模板</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">账号</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">模板类型</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">来源</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">发放时间</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">过期时间</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.couponInstanceId} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="whitespace-nowrap px-4 py-4 font-medium text-slate-900">{item.couponInstanceId}</td>
                      <td className="px-4 py-4 text-slate-900">
                        <div className="font-medium">{item.templateName}</div>
                        <div className="mt-1 text-xs text-slate-500">{item.templateCode}</div>
                      </td>
                      <td className="px-4 py-4 text-slate-600">
                        <div>{item.ownerAccountName || '--'}</div>
                        <div className="mt-1 text-xs text-slate-500">ID: {item.ownerAccountId}</div>
                      </td>
                      <td className="whitespace-nowrap px-4 py-4"><StatusBadge value={item.templateType} /></td>
                      <td className="whitespace-nowrap px-4 py-4"><StatusBadge value={item.sourceType} /></td>
                      <td className="whitespace-nowrap px-4 py-4"><StatusBadge value={item.status} /></td>
                      <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatDateTime(item.issuedAt)}</td>
                      <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatDateTime(item.expiresAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>
      </div>

      {showIssueModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
          <div className="w-full max-w-2xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">手工发券</div>
                <div className="mt-1 text-sm text-slate-500">仅补发到单个账号，保留来源和审计记录。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setShowIssueModal(false)}>
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block lg:col-span-2">
                <span className="mb-2 block text-sm font-medium text-slate-700">券模板</span>
                <select className="input" value={form.templateId} onChange={(event) => setForm((current) => ({ ...current, templateId: Number(event.target.value) }))}>
                  <option value={0}>请选择券模板</option>
                  {activeTemplates.map((item) => (
                    <option key={item.id} value={item.id}>{item.name}（{item.templateCode}）</option>
                  ))}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">目标账号 ID</span>
                <input className="input" type="number" min={1} value={form.ownerAccountId || ''} onChange={(event) => setForm((current) => ({ ...current, ownerAccountId: Number(event.target.value) }))} placeholder="例如 1001" />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">发放张数</span>
                <input className="input" type="number" min={1} step={1} value={form.issueCount} onChange={(event) => setForm((current) => ({ ...current, issueCount: Number(event.target.value) }))} />
              </label>
              <label className="block lg:col-span-2">
                <span className="mb-2 block text-sm font-medium text-slate-700">备注</span>
                <textarea className="textarea" rows={4} value={form.remark || ''} onChange={(event) => setForm((current) => ({ ...current, remark: event.target.value }))} placeholder="可填写补发原因、工单号等。" />
              </label>
            </div>
            <div className="mt-5 flex justify-end">
              <button type="button" className="btn-primary w-full sm:w-auto" onClick={() => void handleManualIssue()} disabled={submitLoading}>
                {submitLoading ? '发放中...' : '确认发券'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
