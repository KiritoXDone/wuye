import { useEffect, useMemo, useState } from 'react'
import { Plus, RefreshCcw, Waves } from 'lucide-react'

import { createFeeRule, getFeeRules } from '@/api/fee-rules'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { FeeRule, FeeRuleCreatePayload, FeeRuleWaterTier } from '@/types/fee-rule'
import { formatDate, formatMoney, formatQuantity } from '@/utils/format'

function createTier(startUsage = 0, endUsage?: number, unitPrice = 0): FeeRuleWaterTier {
  return { startUsage, endUsage, unitPrice }
}

export default function FeeRulesPage() {
  const [communityId, setCommunityId] = useState(100)
  const [list, setList] = useState<FeeRule[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<FeeRuleCreatePayload>({
    communityId: 100,
    feeType: 'PROPERTY',
    unitPrice: 2.5,
    cycleType: 'YEAR',
    pricingMode: 'FLAT',
    effectiveFrom: '',
    effectiveTo: undefined,
    remark: '',
    abnormalAbsThreshold: undefined,
    abnormalMultiplierThreshold: undefined,
    waterTiers: [createTier(0, 5, 2)],
  })

  const propertyRuleCount = useMemo(() => list.filter((item) => item.feeType === 'PROPERTY').length, [list])
  const waterRuleCount = useMemo(() => list.filter((item) => item.feeType === 'WATER').length, [list])
  const tieredRuleCount = useMemo(() => list.filter((item) => item.pricingMode === 'TIERED').length, [list])

  async function loadData(nextCommunityId = communityId) {
    setLoading(true)
    setError('')
    try {
      setList(await getFeeRules(nextCommunityId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '费用规则加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData(communityId)
  }, [])

  function updateTier(index: number, patch: Partial<FeeRuleWaterTier>) {
    setForm((current) => ({
      ...current,
      waterTiers: (current.waterTiers || []).map((tier, currentIndex) => currentIndex === index ? { ...tier, ...patch } : tier),
    }))
  }

  function addTier() {
    const tiers = form.waterTiers || []
    const lastTier = tiers[tiers.length - 1]
    const startUsage = Number(lastTier?.endUsage ?? lastTier?.startUsage ?? 0)
    setForm((current) => ({ ...current, waterTiers: [...(current.waterTiers || []), createTier(startUsage, undefined, Number(current.unitPrice) || 0)] }))
  }

  function removeTier(index: number) {
    if ((form.waterTiers || []).length <= 1) {
      return
    }
    setForm((current) => ({ ...current, waterTiers: (current.waterTiers || []).filter((_, currentIndex) => currentIndex !== index) }))
  }

  function buildPayload(): FeeRuleCreatePayload {
    const isWater = form.feeType === 'WATER'
    return {
      ...form,
      communityId,
      cycleType: isWater ? 'MONTH' : 'YEAR',
      pricingMode: isWater ? form.pricingMode : 'FLAT',
      effectiveTo: form.effectiveTo || undefined,
      abnormalAbsThreshold: isWater ? form.abnormalAbsThreshold : undefined,
      abnormalMultiplierThreshold: isWater ? form.abnormalMultiplierThreshold : undefined,
      waterTiers: isWater && form.pricingMode === 'TIERED'
        ? (form.waterTiers || []).map((tier) => ({
            startUsage: Number(tier.startUsage),
            endUsage: tier.endUsage === undefined || tier.endUsage === '' ? undefined : Number(tier.endUsage),
            unitPrice: Number(tier.unitPrice),
          }))
        : [],
    }
  }

  async function handleCreate() {
    if (!form.effectiveFrom) {
      setError('请选择生效日期。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      const payload = buildPayload()
      await createFeeRule(payload)
      setMessage(`${payload.feeType === 'PROPERTY' ? '年度物业费' : '月度水费'}规则创建成功。`)
      await loadData(communityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建费用规则失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="glass-panel overflow-hidden p-6 sm:p-7">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">计费配置</div>
            <h1 className="mt-3 text-3xl font-semibold text-slate-950">维护年度物业费与月度水费规则，让开单与抄表共享同一套计费基线</h1>
            <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
              当前上线口径中，物业费按年开单、按年缴纳；水费按月抄表并支持阶梯价和异常阈值。请不要再把物业费解释成默认月度账单。
            </p>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新规则'}
          </button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {[
            ['物业费规则', `${propertyRuleCount} 条`],
            ['水费规则', `${waterRuleCount} 条`],
            ['阶梯价规则', `${tieredRuleCount} 条`],
          ].map(([label, value]) => (
            <div key={label} className="glass-soft rounded-[24px] p-4">
              <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
              <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
            </div>
          ))}
        </div>
      </section>

      {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1.1fr_1fr]">
        <PageSection
          title="规则台账"
          description="先按小区查看当前已生效规则，再决定是否新增新的年度物业费或月度水费规则。"
          action={
            <div className="flex flex-wrap gap-2">
              <input className="input w-28" type="number" min={1} value={communityId} onChange={(event) => setCommunityId(Number(event.target.value))} />
              <button type="button" className="btn-primary" onClick={() => void loadData(communityId)} disabled={loading}>查询小区规则</button>
            </div>
          }
        >
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前小区暂无费用规则。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">费种</th>
                    <th className="px-4 py-3 font-medium">周期</th>
                    <th className="px-4 py-3 font-medium">计价方式</th>
                    <th className="px-4 py-3 font-medium">单价</th>
                    <th className="px-4 py-3 font-medium">生效区间</th>
                    <th className="px-4 py-3 font-medium">阈值说明</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4"><StatusBadge value={item.feeType} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.cycleType} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.pricingMode || '--'}</td>
                      <td className="px-4 py-4 font-medium text-slate-900">{formatMoney(item.unitPrice)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDate(item.effectiveFrom)} ~ {formatDate(item.effectiveTo || null, '长期有效')}</td>
                      <td className="px-4 py-4 text-slate-600">
                        {item.feeType === 'WATER'
                          ? `绝对阈值 ${formatQuantity(item.abnormalAbsThreshold)} / 倍数阈值 ${formatQuantity(item.abnormalMultiplierThreshold)}`
                          : '年度物业费默认按面积 × 年单价'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>

        <PageSection title="新增规则" description="物业费默认按年；水费按月并可配置阶梯价和异常阈值。">
          <div className="grid gap-4">
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">小区 ID</span>
                <input className="input" type="number" min={1} value={communityId} onChange={(event) => { const next = Number(event.target.value); setCommunityId(next); setForm((current) => ({ ...current, communityId: next })) }} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">费用类型</span>
                <select className="input" value={form.feeType} onChange={(event) => setForm((current) => ({ ...current, feeType: event.target.value, cycleType: event.target.value === 'WATER' ? 'MONTH' : 'YEAR', pricingMode: event.target.value === 'WATER' ? 'TIERED' : 'FLAT' }))}>
                  <option value="PROPERTY">物业费</option>
                  <option value="WATER">水费</option>
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">单价</span>
                <input className="input" type="number" step="0.0001" value={form.unitPrice} onChange={(event) => setForm((current) => ({ ...current, unitPrice: Number(event.target.value) }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">周期</span>
                <input className="input" value={form.feeType === 'WATER' ? 'MONTH' : 'YEAR'} readOnly />
              </label>
              <label className="block md:col-span-2">
                <span className="mb-2 block text-sm font-medium text-slate-700">生效开始</span>
                <input className="input" type="date" value={form.effectiveFrom} onChange={(event) => setForm((current) => ({ ...current, effectiveFrom: event.target.value }))} />
              </label>
            </div>

            {form.feeType === 'WATER' ? (
              <>
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">绝对阈值</span>
                    <input className="input" type="number" step="0.001" value={form.abnormalAbsThreshold ?? ''} onChange={(event) => setForm((current) => ({ ...current, abnormalAbsThreshold: event.target.value ? Number(event.target.value) : undefined }))} />
                  </label>
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">倍数阈值</span>
                    <input className="input" type="number" step="0.001" value={form.abnormalMultiplierThreshold ?? ''} onChange={(event) => setForm((current) => ({ ...current, abnormalMultiplierThreshold: event.target.value ? Number(event.target.value) : undefined }))} />
                  </label>
                </div>

                <div className="rounded-[24px] border border-slate-200/80 bg-white/75 p-5 backdrop-blur-xl">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                      <Waves className="h-4 w-4 text-cyan-600" />
                      阶梯水价
                    </div>
                    <button type="button" className="btn-secondary gap-2 px-3 py-2" onClick={addTier}>
                      <Plus className="h-4 w-4" />
                      新增档位
                    </button>
                  </div>
                  <div className="mt-4 space-y-3">
                    {(form.waterTiers || []).map((tier, index) => (
                      <div key={index} className="grid gap-3 rounded-2xl border border-slate-100 bg-slate-50/80 p-4 md:grid-cols-[1fr_1fr_1fr_auto]">
                        <input className="input" type="number" step="0.001" value={tier.startUsage} onChange={(event) => updateTier(index, { startUsage: Number(event.target.value) })} placeholder="起始用量" />
                        <input className="input" type="number" step="0.001" value={tier.endUsage ?? ''} onChange={(event) => updateTier(index, { endUsage: event.target.value ? Number(event.target.value) : undefined })} placeholder="结束用量（最后一档可空）" />
                        <input className="input" type="number" step="0.0001" value={tier.unitPrice} onChange={(event) => updateTier(index, { unitPrice: Number(event.target.value) })} placeholder="单价" />
                        <button type="button" className="btn-secondary px-3 py-2" onClick={() => removeTier(index)} disabled={(form.waterTiers || []).length <= 1}>删除</button>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <div className="glass-soft rounded-[24px] p-5 text-sm leading-7 text-slate-600">
                年度物业费应以房间面积 × 年单价计算，并按自然年开单，不再沿用“年费折月”的旧展示口径。
              </div>
            )}

            <button type="button" className="btn-primary w-full" onClick={() => void handleCreate()} disabled={submitLoading}>
              {submitLoading ? '提交中...' : '创建费用规则'}
            </button>
          </div>
        </PageSection>
      </div>
    </div>
  )
}
