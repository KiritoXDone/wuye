import { useEffect, useMemo, useState } from 'react'
import { CalendarRange, ReceiptText, Waves } from 'lucide-react'

import { generatePropertyBill, generateWaterBill } from '@/api/billing'
import { getCommunities } from '@/api/communities'
import PageSection from '@/components/ui/PageSection'
import type { AdminCommunity } from '@/types/community'

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

export default function BillingGeneratePage() {
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [selectedCommunityId, setSelectedCommunityId] = useState<number>(0)
  const [propertyForm, setPropertyForm] = useState({
    communityId: 0,
    year: currentYear,
    overwriteStrategy: 'SKIP',
  })
  const [waterForm, setWaterForm] = useState({
    communityId: 0,
    year: currentYear,
    month: currentMonth,
    overwriteStrategy: 'SKIP',
  })
  const [propertyResult, setPropertyResult] = useState('')
  const [waterResult, setWaterResult] = useState('')
  const [propertyLoading, setPropertyLoading] = useState(false)
  const [waterLoading, setWaterLoading] = useState(false)
  const [propertyError, setPropertyError] = useState('')
  const [waterError, setWaterError] = useState('')

  const monthOptions = useMemo(() => Array.from({ length: 12 }, (_, index) => index + 1), [])

  useEffect(() => {
    void getCommunities().then((list) => {
      setCommunities(list)
      const initialCommunityId = list[0]?.id ?? 0
      setSelectedCommunityId(initialCommunityId)
      setPropertyForm((current) => ({ ...current, communityId: initialCommunityId }))
      setWaterForm((current) => ({ ...current, communityId: initialCommunityId }))
    })
  }, [])

  async function handlePropertyGenerate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPropertyLoading(true)
    setPropertyError('')
    setPropertyResult('')
    if (!propertyForm.communityId) {
      setPropertyError('请先选择小区。')
      setPropertyLoading(false)
      return
    }
    try {
      const result = await generatePropertyBill(propertyForm)
      setPropertyResult(`已完成 ${propertyForm.year} 年物业费开单，本次生成 ${result.generatedCount} 张账单。`)
    } catch (error) {
      setPropertyError(error instanceof Error ? error.message : '年度物业费开单失败')
    } finally {
      setPropertyLoading(false)
    }
  }

  async function handleWaterGenerate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWaterLoading(true)
    setWaterError('')
    setWaterResult('')
    if (!waterForm.communityId) {
      setWaterError('请先选择小区。')
      setWaterLoading(false)
      return
    }
    try {
      const result = await generateWaterBill(waterForm)
      setWaterResult(`已尝试生成 ${waterForm.year}-${String(waterForm.month).padStart(2, '0')} 水费账单，本次生成 ${result.generatedCount} 张账单。`)
    } catch (error) {
      setWaterError(error instanceof Error ? error.message : '水费账单生成失败')
    } finally {
      setWaterLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">开账中心</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">开账中心</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">先选择目标小区，再执行物业费年度开单或水费补出账，避免手填错误的小区 ID。</p>
          </div>
          <div className="min-w-[240px]">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">目标小区</span>
              <select
                className="input"
                value={selectedCommunityId || ''}
                onChange={(event) => {
                  const nextCommunityId = Number(event.target.value) || 0
                  setSelectedCommunityId(nextCommunityId)
                  setPropertyForm((current) => ({ ...current, communityId: nextCommunityId }))
                  setWaterForm((current) => ({ ...current, communityId: nextCommunityId }))
                }}
              >
                <option value="">请选择小区</option>
                {communities.map((community) => (
                  <option key={community.id} value={community.id}>{community.name}</option>
                ))}
              </select>
            </label>
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-2">
        <PageSection
          title="年度物业费开单"
          description="按自然年生成。"
          action={<span className="tag">按年</span>}
        >
          <form className="grid gap-5" onSubmit={handlePropertyGenerate}>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">当前小区</span>
                <input className="input" value={communities.find((item) => item.id === propertyForm.communityId)?.name || '请先选择小区'} readOnly />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">自然年</span>
                <input
                  className="input"
                  type="number"
                  min={2020}
                  max={2100}
                  value={propertyForm.year}
                  onChange={(event) => setPropertyForm((current) => ({ ...current, year: Number(event.target.value) }))}
                />
              </label>
            </div>

            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">重复账单处理策略</span>
              <select
                className="input"
                value={propertyForm.overwriteStrategy}
                onChange={(event) => setPropertyForm((current) => ({ ...current, overwriteStrategy: event.target.value }))}
              >
                <option value="SKIP">SKIP - 保留既有有效账单</option>
                <option value="FAIL">FAIL - 发现重复直接失败</option>
              </select>
            </label>

            {propertyError ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{propertyError}</div> : null}
            {propertyResult ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{propertyResult}</div> : null}

            <button type="submit" className="btn-primary w-full sm:w-auto" disabled={propertyLoading}>
              {propertyLoading ? '开单中...' : '生成年度物业费账单'}
            </button>
          </form>
        </PageSection>

        <PageSection
          title="水费补出账"
          description="补齐指定账期。"
          action={<span className="tag">按月</span>}
        >
          <form className="grid gap-5" onSubmit={handleWaterGenerate}>
            <div className="grid gap-4 md:grid-cols-3">
              <label className="block md:col-span-1">
                <span className="mb-2 block text-sm font-medium text-slate-700">当前小区</span>
                <input className="input" value={communities.find((item) => item.id === waterForm.communityId)?.name || '请先选择小区'} readOnly />
              </label>
              <label className="block md:col-span-1">
                <span className="mb-2 block text-sm font-medium text-slate-700">年份</span>
                <input
                  className="input"
                  type="number"
                  min={2020}
                  max={2100}
                  value={waterForm.year}
                  onChange={(event) => setWaterForm((current) => ({ ...current, year: Number(event.target.value) }))}
                />
              </label>
              <label className="block md:col-span-1">
                <span className="mb-2 block text-sm font-medium text-slate-700">月份</span>
                <select
                  className="input"
                  value={waterForm.month}
                  onChange={(event) => setWaterForm((current) => ({ ...current, month: Number(event.target.value) }))}
                >
                  {monthOptions.map((month) => (
                    <option key={month} value={month}>
                      {month} 月
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">重复账单处理策略</span>
              <select
                className="input"
                value={waterForm.overwriteStrategy}
                onChange={(event) => setWaterForm((current) => ({ ...current, overwriteStrategy: event.target.value }))}
              >
                <option value="SKIP">SKIP - 仅补齐缺失账单</option>
                <option value="FAIL">FAIL - 发现重复直接失败</option>
              </select>
            </label>

            {waterError ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{waterError}</div> : null}
            {waterResult ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{waterResult}</div> : null}

            <button type="submit" className="btn-secondary w-full gap-2 sm:w-auto" disabled={waterLoading}>
              <Waves className="h-4 w-4" />
              {waterLoading ? '生成中...' : '补齐当月水费账单'}
            </button>
          </form>
        </PageSection>
      </div>
    </div>
  )
}
