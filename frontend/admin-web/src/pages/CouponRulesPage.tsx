import { useEffect, useMemo, useState } from 'react'

import { createCouponRule, getCouponRules, getCouponTemplates } from '@/api/coupons'
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
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<CouponRuleCreatePayload>({
    name: '物业费支付成功送停车券',
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
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '发券规则创建失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="glass-panel overflow-hidden p-6 sm:p-7">
        <div>
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">券体系</div>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">配置支付成功后的奖励券发放规则，保持模板与规则分层</h1>
          <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
            当前页面仅做最小新增与列表核对。创建时提交 templateCode，列表返回 templateId；若需更强映射能力，应后续补后端 VO。
          </p>
        </div>
      </section>

      {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
        <PageSection title="规则列表" description="列表直接展示后端当前规则，方便联调支付成功后的奖励券发放。">
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无发券规则。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">规则名称</th>
                    <th className="px-4 py-3 font-medium">费种</th>
                    <th className="px-4 py-3 font-medium">模板 ID</th>
                    <th className="px-4 py-3 font-medium">最低实付金额</th>
                    <th className="px-4 py-3 font-medium">发券数量</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.ruleName}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.templateId}</td>
                      <td className="px-4 py-4 text-right text-slate-900">{formatMoney(item.minPayAmount)}</td>
                      <td className="px-4 py-4 text-right text-slate-600">{item.rewardCount}</td>
                      <td className="px-4 py-4"><StatusBadge value={String(item.status)} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>

        <PageSection title="新增规则" description="奖励券模板下拉仅展示 VOUCHER 类型模板。">
          <div className="grid gap-4">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">规则名称</span>
              <input className="input" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">费种</span>
              <select className="input" value={form.feeType} onChange={(event) => setForm((current) => ({ ...current, feeType: event.target.value }))}>
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
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">发券数量</span>
              <input className="input" type="number" min={1} step={1} value={form.rewardCount} onChange={(event) => setForm((current) => ({ ...current, rewardCount: Number(event.target.value) }))} />
            </label>
            <button type="button" className="btn-primary w-full" onClick={() => void handleCreate()} disabled={submitLoading}>
              {submitLoading ? '保存中...' : '保存规则'}
            </button>
          </div>
        </PageSection>
      </div>
    </div>
  )
}
