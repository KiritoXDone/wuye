import { useEffect, useMemo, useState } from 'react'
import { Plus, RefreshCcw, Waves } from 'lucide-react'

import { getCommunities } from '@/api/communities'
import { createFeeRule, getFeeRules } from '@/api/fee-rules'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { AdminCommunity } from '@/types/community'
import type { FeeRule, FeeRuleCreatePayload, FeeRuleWaterTier } from '@/types/fee-rule'
import { formatDate, formatMoney, formatQuantity } from '@/utils/format'

function createTier(startUsage = 0, endUsage?: number, unitPrice = 0): FeeRuleWaterTier {
  return { startUsage, endUsage, unitPrice }
}

export default function FeeRulesPage() {
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [communityId, setCommunityId] = useState<number>(0)
  const [list, setList] = useState<FeeRule[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<FeeRuleCreatePayload>({
    communityId: 0,
    feeType: 'PROPERTY',
    unitPrice: 2.5,
    cycleType: 'YEAR',
    pricingMode: 'FLAT',
    effectiveFrom: '',
    effectiveTo: undefined,
    remark: '',
    waterTiers: [createTier(0, 5, 2)],
  })

  const selectedCommunity = useMemo(() => communities.find((item) => item.id === communityId), [communities, communityId])
  const propertyRuleCount = useMemo(() => list.filter((item) => item.feeType === 'PROPERTY').length, [list])
  const waterRuleCount = useMemo(() => list.filter((item) => item.feeType === 'WATER').length, [list])
  const tieredRuleCount = useMemo(() => list.filter((item) => item.pricingMode === 'TIERED').length, [list])

  async function loadData(nextCommunityId = communityId) {
    if (!nextCommunityId) {
      setList([])
      return
    }
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

  async function loadCommunitiesAndRules() {
    setLoading(true)
    setError('')
    try {
      const communityList = await getCommunities()
      setCommunities(communityList)
      const firstCommunityId = communityList[0]?.id || 0
      setCommunityId(firstCommunityId)
      setForm((current) => ({ ...current, communityId: firstCommunityId }))
      if (firstCommunityId) {
        setList(await getFeeRules(firstCommunityId))
      } else {
        setList([])
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '费用规则加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadCommunitiesAndRules()
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
    if (!communityId) {
      setError('请先选择小区。')
      return
    }
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
      <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">计费配置</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">费用规则</h1>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          {[
            ['物业费规则', `${propertyRuleCount} 条`],
            ['水费规则', `${waterRuleCount} 条`],
            ['阶梯价规则', `${tieredRuleCount} 条`],
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

      <div className="grid gap-6 xl:grid-cols-[1.1fr_1fr]">
        <PageSection
          title="规则台账"
          description="按小区查看当前规则。"
          action={
            <div className="flex flex-wrap gap-2">
              <select
                className="input min-w-[220px]"
                value={communityId || ''}
                onChange={(event) => {
                  const nextCommunityId = Number(event.target.value) || 0
                  setCommunityId(nextCommunityId)
                  setForm((current) => ({ ...current, communityId: nextCommunityId }))
                }}
              >
                <option value="">请选择小区</option>
                {communities.map((community) => (
                  <option key={community.id} value={community.id}>{community.name}</option>
                ))}
              </select>
              <button type="button" className="btn-primary" onClick={() => void loadData(communityId)} disabled={loading || !communityId}>查询小区规则</button>
            </div>
          }
        >
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前小区暂无费用规则。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">小区</th>
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
                      <td className="px-4 py-4 text-slate-600">{selectedCommunity?.name || item.communityName || `小区 ${item.communityId}`}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.cycleType} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.pricingMode || '--'}</td>
                      <td className="px-4 py-4 font-medium text-slate-900">{formatMoney(item.unitPrice)}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDate(item.effectiveFrom)} ~ {formatDate(item.effectiveTo || null, '长期有效')}</td>
                      <td className="px-4 py-4 text-slate-600">
                        {item.feeType === 'WATER'
                          ? (item.pricingMode === 'TIERED' ? '按阶梯水价执行' : '按固定单价执行')
                          : '年度物业费默认按面积 × 年单价'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>

        <PageSection title="新增规则" description={selectedCommunity ? `为 ${selectedCommunity.name} 创建新规则。` : '创建新规则。'}>
          <div className="grid gap-4">
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">所属小区</span>
                <input className="input" value={selectedCommunity?.name || '请先选择小区'} readOnly />
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
              <div className="rounded-2xl border border-slate-200 bg-white p-5">
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
                      <div key={index} className="grid gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 md:grid-cols-[1fr_1fr_1fr_auto]">
                        <input className="input" type="number" step="0.001" value={tier.startUsage} onChange={(event) => updateTier(index, { startUsage: Number(event.target.value) })} placeholder="起始用量" />
                        <input className="input" type="number" step="0.001" value={tier.endUsage ?? ''} onChange={(event) => updateTier(index, { endUsage: event.target.value ? Number(event.target.value) : undefined })} placeholder="结束用量（最后一档可空）" />
                        <input className="input" type="number" step="0.0001" value={tier.unitPrice} onChange={(event) => updateTier(index, { unitPrice: Number(event.target.value) })} placeholder="单价" />
                        <button type="button" className="btn-secondary px-3 py-2" onClick={() => removeTier(index)} disabled={(form.waterTiers || []).length <= 1}>删除</button>
                      </div>
                    ))}
                  </div>
              </div>
            ) : null}

            <button type="button" className="btn-primary w-full" onClick={() => void handleCreate()} disabled={submitLoading}>
              {submitLoading ? '提交中...' : '创建费用规则'}
            </button>
          </div>
        </PageSection>
      </div>
    </div>
  )
}
