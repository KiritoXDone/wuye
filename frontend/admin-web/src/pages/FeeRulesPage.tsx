import { useEffect, useMemo, useState } from 'react'
import { Plus, RefreshCcw, Trash2, Waves, X } from 'lucide-react'

import { getCommunities } from '@/api/communities'
import { createFeeRule, deleteFeeRule, getFeeRules } from '@/api/fee-rules'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { pricingModeOptions } from '@/constants/options'
import type { AdminCommunity } from '@/types/community'
import type { FeeRule, FeeRuleCreatePayload, FeeRuleWaterTier } from '@/types/fee-rule'
import { formatMoney } from '@/utils/format'

function createTier(startUsage = 0, endUsage?: number, unitPrice = 0): FeeRuleWaterTier {
  return { startUsage, endUsage, unitPrice }
}

function pricingModeLabel(value?: string) {
  return value === 'TIERED' ? '阶梯水价' : value === 'FLAT' ? '固定单价' : '--'
}

function cycleLabel(feeType: string) {
  return feeType === 'WATER' ? '按月' : '按年'
}

export default function FeeRulesPage() {
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [viewCommunityId, setViewCommunityId] = useState<number>(0)
  const [createCommunityId, setCreateCommunityId] = useState<number>(0)
  const [list, setList] = useState<FeeRule[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [listError, setListError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<FeeRuleCreatePayload>({
    communityId: 0,
    feeType: 'PROPERTY',
    unitPrice: 2.5,
    cycleType: 'YEAR',
    pricingMode: 'FLAT',
    effectiveFrom: '',
    remark: '',
    waterTiers: [createTier(0, 5, 2)],
  })

  const selectedViewCommunity = useMemo(() => communities.find((item) => item.id === viewCommunityId), [communities, viewCommunityId])
  const selectedCreateCommunity = useMemo(() => communities.find((item) => item.id === createCommunityId), [communities, createCommunityId])
  const propertyRuleCount = useMemo(() => list.filter((item) => item.feeType === 'PROPERTY').length, [list])
  const waterRuleCount = useMemo(() => list.filter((item) => item.feeType === 'WATER').length, [list])
  const tieredRuleCount = useMemo(() => list.filter((item) => item.pricingMode === 'TIERED').length, [list])

  async function loadData(nextCommunityId = viewCommunityId) {
    if (!nextCommunityId) {
      setList([])
      return
    }
    setLoading(true)
    setListError('')
    try {
      setList(await getFeeRules(nextCommunityId))
    } catch (err) {
      setListError(err instanceof Error ? err.message : '费用规则加载失败')
    } finally {
      setLoading(false)
    }
  }

  async function loadCommunities() {
    setLoading(true)
    setListError('')
    try {
      const communityList = await getCommunities()
      setCommunities(communityList)
      const firstCommunityId = communityList[0]?.id || 0
      setViewCommunityId(firstCommunityId)
      setCreateCommunityId(firstCommunityId)
      if (firstCommunityId) {
        setList(await getFeeRules(firstCommunityId))
      } else {
        setList([])
      }
    } catch (err) {
      setListError(err instanceof Error ? err.message : '费用规则加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadCommunities()
  }, [])

  useEffect(() => {
    if (!viewCommunityId || !communities.length) return
    void loadData(viewCommunityId)
  }, [viewCommunityId])

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
    if ((form.waterTiers || []).length <= 1) return
    setForm((current) => ({ ...current, waterTiers: (current.waterTiers || []).filter((_, currentIndex) => currentIndex !== index) }))
  }

  function buildPayload(): FeeRuleCreatePayload {
    const isWater = form.feeType === 'WATER'
    const pricingMode = isWater ? form.pricingMode || 'FLAT' : 'FLAT'
    return {
      ...form,
      communityId: createCommunityId,
      cycleType: isWater ? 'MONTH' : 'YEAR',
      pricingMode,
      waterTiers: isWater && pricingMode === 'TIERED'
        ? (form.waterTiers || []).map((tier) => ({
            startUsage: Number(tier.startUsage),
            endUsage: tier.endUsage === undefined || tier.endUsage === '' ? undefined : Number(tier.endUsage),
            unitPrice: Number(tier.unitPrice),
          }))
        : [],
    }
  }

  async function handleCreate() {
    if (!createCommunityId) {
      setSubmitError('请选择规则所属小区。')
      return
    }
    if (!form.effectiveFrom) {
      setSubmitError('请选择生效开始日期。')
      return
    }
    setSubmitLoading(true)
    setSubmitError('')
    setMessage('')
    try {
      const payload = buildPayload()
      await createFeeRule(payload)
      setShowCreate(false)
      if (createCommunityId === viewCommunityId) {
        await loadData(viewCommunityId)
        setMessage(`${payload.feeType === 'PROPERTY' ? '物业费' : '水费'}规则创建成功。`)
      } else {
        setMessage(`${payload.feeType === 'PROPERTY' ? '物业费' : '水费'}规则创建成功，可切换到对应小区查看。`)
      }
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : '创建费用规则失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  async function handleDelete(rule: FeeRule) {
    if (!window.confirm(`确认删除这条${rule.feeType === 'PROPERTY' ? '物业费' : '水费'}规则吗？删除后不可恢复。`)) return
    setDeletingId(rule.id)
    setListError('')
    setMessage('')
    try {
      await deleteFeeRule(rule.id)
      await loadData(viewCommunityId)
      setMessage('费用规则已删除。')
    } catch (err) {
      setListError(err instanceof Error ? err.message : '删除费用规则失败')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">计费配置</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">费用规则</h1>
              <p className="mt-2 text-sm text-slate-500">如需调整规则，请删除旧规则后重新新增。水费支持固定单价或阶梯水价两种模式。</p>
            </div>
            <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading || !viewCommunityId}>
              <RefreshCcw className="h-4 w-4" />
              {loading ? '刷新中...' : '刷新当前规则'}
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

        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection
          title="规则台账"
          description={selectedViewCommunity ? `切换小区后自动查看 ${selectedViewCommunity.name} 的当前规则。` : '切换小区后自动查看当前规则。'}
          action={<div className="flex gap-2"><select className="input w-[170px] sm:w-[190px]" value={viewCommunityId || ''} onChange={(event) => setViewCommunityId(Number(event.target.value) || 0)}><option value="">查看小区</option>{communities.map((community) => <option key={community.id} value={community.id}>{community.name}</option>)}</select><button type="button" className="btn-primary shrink-0 whitespace-nowrap gap-2" onClick={() => setShowCreate(true)}><Plus className="h-4 w-4" />新增规则</button></div>}
        >
          <AsyncState loading={loading} error={listError} empty={!list.length} emptyDescription="当前小区暂无费用规则。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead><tr className="border-b border-slate-200 text-slate-500"><th className="px-4 py-3 font-medium">小区</th><th className="px-4 py-3 font-medium">费种</th><th className="px-4 py-3 font-medium">周期</th><th className="px-4 py-3 font-medium">计价方式</th><th className="px-4 py-3 font-medium">单价</th><th className="px-4 py-3 font-medium">说明</th><th className="px-4 py-3 font-medium text-right">操作</th></tr></thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 text-slate-600">{selectedViewCommunity?.name || item.communityName || `小区 ${item.communityId}`}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.cycleType} /></td>
                      <td className="px-4 py-4 text-slate-600">{pricingModeLabel(item.pricingMode)}</td>
                      <td className="px-4 py-4 font-medium text-slate-900">{formatMoney(item.unitPrice)}</td>
                      <td className="px-4 py-4 text-slate-600">{item.feeType === 'WATER' ? (item.pricingMode === 'TIERED' ? '按阶梯水价执行' : '整段按同一单价执行') : '年度物业费默认按面积 × 年单价'}</td>
                      <td className="px-4 py-4 text-right"><button type="button" className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => void handleDelete(item)} disabled={deletingId === item.id}><Trash2 className="h-4 w-4" />{deletingId === item.id ? '删除中' : '删除'}</button></td>
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
                <div className="text-lg font-semibold text-slate-950">新增规则</div>
                <div className="mt-1 text-sm text-slate-500">为对应小区新增费用规则。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setShowCreate(false)}><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">所属小区</span><select className="input" value={createCommunityId || ''} onChange={(event) => setCreateCommunityId(Number(event.target.value) || 0)}><option value="">请选择所属小区</option>{communities.map((community) => <option key={community.id} value={community.id}>{community.name}</option>)}</select></label>
              <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">费用类型</span><select className="input" value={form.feeType} onChange={(event) => setForm((current) => ({ ...current, feeType: event.target.value, cycleType: event.target.value === 'WATER' ? 'MONTH' : 'YEAR', pricingMode: 'FLAT' }))}><option value="PROPERTY">物业费</option><option value="WATER">水费</option></select></label>
              <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">单价</span><input className="input" type="number" step="0.0001" value={form.unitPrice} onChange={(event) => setForm((current) => ({ ...current, unitPrice: Number(event.target.value) }))} /></label>
              <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">计费周期</span><input className="input" value={cycleLabel(form.feeType)} readOnly /></label>
              <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">生效开始日期</span><input className="input" type="date" value={form.effectiveFrom} onChange={(event) => setForm((current) => ({ ...current, effectiveFrom: event.target.value }))} /></label>
              <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">生效结束日期</span><input className="input" type="date" value={form.effectiveTo || ''} onChange={(event) => setForm((current) => ({ ...current, effectiveTo: event.target.value || undefined }))} /></label>
              {form.feeType === 'WATER' ? <label className="block lg:col-span-2"><span className="mb-2 block text-sm font-medium text-slate-700">水费模式</span><select className="input" value={form.pricingMode || 'FLAT'} onChange={(event) => setForm((current) => ({ ...current, pricingMode: event.target.value }))}>{pricingModeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label> : null}
            </div>
            {submitError ? <div className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{submitError}</div> : null}
            {form.feeType === 'WATER' && form.pricingMode === 'TIERED' ? <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-5"><div className="flex items-center justify-between gap-3"><div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><Waves className="h-4 w-4 text-cyan-600" />阶梯水价</div><button type="button" className="btn-secondary px-3 py-2" onClick={addTier}>新增档位</button></div><div className="mt-4 space-y-3">{(form.waterTiers || []).map((tier, index) => <div key={index} className="grid gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 md:grid-cols-[1fr_1fr_1fr_auto]"><input className="input" type="number" step="0.001" value={tier.startUsage} onChange={(event) => updateTier(index, { startUsage: Number(event.target.value) })} placeholder="起始用量" /><input className="input" type="number" step="0.001" value={tier.endUsage ?? ''} onChange={(event) => updateTier(index, { endUsage: event.target.value ? Number(event.target.value) : undefined })} placeholder="结束用量（最后一档可空）" /><input className="input" type="number" step="0.0001" value={tier.unitPrice} onChange={(event) => updateTier(index, { unitPrice: Number(event.target.value) })} placeholder="单价" /><button type="button" className="btn-secondary px-3 py-2" onClick={() => removeTier(index)} disabled={(form.waterTiers || []).length <= 1}>删除档位</button></div>)}</div></div> : null}
            <div className="mt-5 flex justify-end"><button type="button" className="btn-primary w-full sm:w-auto" onClick={() => void handleCreate()} disabled={submitLoading}>{submitLoading ? '提交中...' : '确认新增规则'}</button></div>
          </div>
        </div>
      ) : null}
    </>
  )
}
