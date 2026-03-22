import { useEffect, useState } from 'react'
import { Plus, Trash2, X } from 'lucide-react'

import { createCouponTemplate, deleteCouponTemplate, getCouponTemplates } from '@/api/coupons'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { couponTemplateTypeOptions, discountModeOptions, feeTypeOptions } from '@/constants/options'
import type { CouponTemplate, CouponTemplateCreatePayload } from '@/types/coupon'
import { formatDateTime, formatMoney } from '@/utils/format'

export default function CouponTemplatesPage() {
  const [list, setList] = useState<CouponTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<CouponTemplateCreatePayload>({
    templateCode: 'PAY-OFF-10',
    type: 'PAYMENT',
    feeType: 'PROPERTY',
    name: '满100减10物业券',
    discountMode: 'FIXED',
    valueAmount: 10,
    thresholdAmount: 100,
    validFrom: '2026-01-01 00:00:00',
    validTo: '2026-12-31 23:59:59',
  })

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      setList(await getCouponTemplates())
    } catch (err) {
      setError(err instanceof Error ? err.message : '券模板加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  async function handleCreate() {
    if (!form.templateCode.trim() || !form.name.trim()) {
      setError('请填写模板编码和券名称。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      await createCouponTemplate({ ...form, feeType: form.feeType || undefined })
      setMessage('券模板创建成功。')
      setShowCreate(false)
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '券模板创建失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  async function handleDelete(item: CouponTemplate) {
    if (!window.confirm(`确认停用券模板“${item.name}”吗？停用后不再允许新发放和新使用，但会保留历史券实例。`)) {
      return
    }
    setDeletingId(item.id)
    setError('')
    setMessage('')
    try {
      await deleteCouponTemplate(item.id)
      setMessage('券模板已停用。')
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '停用券模板失败')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">券体系</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">券模板</h1>
          </div>
        </section>

        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection title="模板列表" description="查看当前模板。" action={<button type="button" className="btn-primary gap-2" onClick={() => setShowCreate(true)}><Plus className="h-4 w-4" />新增模板</button>}>
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无券模板。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="whitespace-nowrap px-4 py-3 font-medium">模板编码</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">券名称</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">类型</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">费种</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">抵扣模式</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">抵扣值</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">门槛</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">有效期</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium text-right">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.templateCode}</td>
                      <td className="px-4 py-4 text-slate-900">{item.name}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.type} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType || 'ALL'} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.discountMode} /></td>
                      <td className="px-4 py-4 text-right text-slate-900">{formatMoney(item.valueAmount)}</td>
                      <td className="px-4 py-4 text-right text-slate-600">{formatMoney(item.thresholdAmount)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDateTime(item.validFrom)} ~ {formatDateTime(item.validTo)}</td>
                      <td className="px-4 py-4 text-right">
                        <button type="button" className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => void handleDelete(item)} disabled={deletingId === item.id}>
                          <Trash2 className="h-4 w-4" />
                          {deletingId === item.id ? '停用中' : '停用'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>
      </div>

      {showCreate ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
          <div className="w-full max-w-3xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">新增模板</div>
                <div className="mt-1 text-sm text-slate-500">新模板会出现在列表中。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setShowCreate(false)}><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">模板编码</span>
                <input className="input" value={form.templateCode} onChange={(event) => setForm((current) => ({ ...current, templateCode: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">券类型</span>
                <select className="input" value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}>
                  {couponTemplateTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">适用费种</span>
                <select className="input" value={form.feeType || ''} onChange={(event) => setForm((current) => ({ ...current, feeType: event.target.value || undefined }))}>
                  <option value="">不限定</option>
                  {feeTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">券名称</span>
                <input className="input" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">抵扣模式</span>
                <select className="input min-w-[140px]" value={form.discountMode} onChange={(event) => setForm((current) => ({ ...current, discountMode: event.target.value }))}>
                  {discountModeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">抵扣值</span>
                <input className="input" type="number" min={0} step={0.01} value={form.valueAmount} onChange={(event) => setForm((current) => ({ ...current, valueAmount: Number(event.target.value) }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">使用门槛</span>
                <input className="input" type="number" min={0} step={0.01} value={form.thresholdAmount} onChange={(event) => setForm((current) => ({ ...current, thresholdAmount: Number(event.target.value) }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">开始时间</span>
                <input className="input" value={form.validFrom} onChange={(event) => setForm((current) => ({ ...current, validFrom: event.target.value }))} placeholder="YYYY-MM-DD HH:mm:ss" />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">结束时间</span>
                <input className="input" value={form.validTo} onChange={(event) => setForm((current) => ({ ...current, validTo: event.target.value }))} placeholder="YYYY-MM-DD HH:mm:ss" />
              </label>
            </div>
            <div className="mt-5 flex justify-end">
              <button type="button" className="btn-primary w-full sm:w-auto" onClick={() => void handleCreate()} disabled={submitLoading}>
                {submitLoading ? '保存中...' : '保存模板'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
