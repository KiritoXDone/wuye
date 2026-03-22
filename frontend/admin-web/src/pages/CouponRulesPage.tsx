import { useEffect, useMemo, useState } from 'react'
import { Plus, Trash2, X } from 'lucide-react'

import { createCouponRule, deleteCouponRule, getCouponRules, getCouponTemplates } from '@/api/coupons'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { feeTypeOptions } from '@/constants/options'
import type { CouponRule, CouponRuleCreatePayload, CouponTemplate } from '@/types/coupon'
import { formatMoney } from '@/utils/format'

export default function CouponRulesPage() {
  const [list, setList] = useState<CouponRule[]>([])
  const [templates, setTemplates] = useState<CouponTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<CouponRuleCreatePayload>({
    name: '物业费支付成功送停车券',
    triggerType: 'PROPERTY_PAYMENT',
    feeType: 'PROPERTY',
    templateCode: 'VCH-PARK-1H',
    minPayAmount: 100,
    rewardCount: 1,
  })

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      const [rulesResult, templatesResult] = await Promise.all([getCouponRules(), getCouponTemplates()])
      setList(rulesResult)
      setTemplates(templatesResult.filter((item) => item.type === 'VOUCHER'))
    } catch (err) {
      setError(err instanceof Error ? err.message : '发券规则加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  const selectedTemplateOptions = useMemo(() => templates.map((template) => ({ label: `${template.name}（${template.templateCode}）`, value: template.templateCode })), [templates])

  async function handleCreate() {
    if (!form.name.trim() || !form.templateCode.trim()) {
      setError('请填写规则名称并选择奖励券模板。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      await createCouponRule(form)
      setMessage('发券规则创建成功。')
      setShowCreate(false)
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '发券规则创建失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  async function handleDelete(item: CouponRule) {
    if (!window.confirm(`确认删除发券规则“${item.ruleName}”吗？删除后不可恢复。`)) {
      return
    }
    setDeletingId(item.id)
    setError('')
    setMessage('')
    try {
      await deleteCouponRule(item.id)
      setMessage('发券规则已停用。')
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除发券规则失败')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">券体系</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">发券规则</h1>
            </div>
          </div>
        </section>

        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection
          title="规则列表"
          description="查看当前规则。"
          action={<button type="button" className="btn-primary gap-2" onClick={() => setShowCreate(true)}><Plus className="h-4 w-4" />新增规则</button>}
        >
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无发券规则。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="whitespace-nowrap px-4 py-3 font-medium">规则名称</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">费种</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">模板 ID</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">规则说明</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">最低实付金额</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">发券数量</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium text-right">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.ruleName}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.templateId}</td>
                      <td className="px-4 py-4 text-slate-600">支付{item.feeType === 'PROPERTY' ? '物业费' : '水费'}且实付满 {formatMoney(item.minPayAmount)} 时发 {item.rewardCount} 张奖励券</td>
                      <td className="px-4 py-4 text-right text-slate-900">{formatMoney(item.minPayAmount)}</td>
                      <td className="px-4 py-4 text-right text-slate-600">{item.rewardCount}</td>
                      <td className="px-4 py-4 text-right">
                        <button type="button" className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => void handleDelete(item)} disabled={deletingId === item.id}>
                          <Trash2 className="h-4 w-4" />
                          {deletingId === item.id ? '删除中' : '删除'}
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
          <div className="w-full max-w-2xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">新增规则</div>
                <div className="mt-1 text-sm text-slate-500">当前仅支持按费种、最低实付金额和奖励张数配置发券规则。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setShowCreate(false)}>
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">规则名称</span>
                <input className="input" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">费种</span>
                <select className="input min-w-[140px]" value={form.feeType} onChange={(event) => setForm((current) => ({ ...current, feeType: event.target.value }))}>
                  {feeTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">奖励券模板</span>
                <select className="input" value={form.templateCode} onChange={(event) => setForm((current) => ({ ...current, templateCode: event.target.value }))}>
                  {selectedTemplateOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">最低实付金额</span>
                <input className="input" type="number" min={0} step={0.01} value={form.minPayAmount} onChange={(event) => setForm((current) => ({ ...current, minPayAmount: Number(event.target.value) }))} />
              </label>
              <label className="block lg:col-span-2">
                <span className="mb-2 block text-sm font-medium text-slate-700">发券数量</span>
                <input className="input" type="number" min={1} step={1} value={form.rewardCount} onChange={(event) => setForm((current) => ({ ...current, rewardCount: Number(event.target.value) }))} />
              </label>
            </div>
            <div className="mt-5 flex justify-end">
              <button type="button" className="btn-primary w-full sm:w-auto" onClick={() => void handleCreate()} disabled={submitLoading}>
                {submitLoading ? '保存中...' : '保存规则'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
