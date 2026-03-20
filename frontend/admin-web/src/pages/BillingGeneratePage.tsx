import { useMemo, useState } from 'react'
import { CalendarRange, ReceiptText, Waves } from 'lucide-react'

import { generatePropertyBill, generateWaterBill } from '@/api/billing'
import PageSection from '@/components/ui/PageSection'

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

export default function BillingGeneratePage() {
  const [propertyForm, setPropertyForm] = useState({
    communityId: 1,
    year: currentYear,
    overwriteStrategy: 'SKIP',
  })
  const [waterForm, setWaterForm] = useState({
    communityId: 1,
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

  async function handlePropertyGenerate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPropertyLoading(true)
    setPropertyError('')
    setPropertyResult('')
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
      <section className="glass-panel overflow-hidden p-6 sm:p-7">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">年度开单中心</div>
            <h1 className="mt-3 text-3xl font-semibold text-slate-950">按自然年生成物业费，按账期补齐水费批量出账</h1>
            <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
              当前后台以房间为账单主体。物业费默认按年开单，水费仍以月度抄表为主，这里保留批量出账入口用于运营补齐与复核。
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            {[
              ['账单主体', '房间', '同一房间共享支付结果'],
              ['物业费周期', '自然年', '一房一年一张有效账单'],
              ['水费周期', '自然月', '录入抄表后立即出账'],
            ].map(([label, value, hint]) => (
              <div key={label} className="glass-soft rounded-[24px] p-4">
                <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
                <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
                <div className="mt-1 text-sm leading-6 text-slate-500">{hint}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-2">
        <PageSection
          title="年度物业费开单"
          description="面向收费运营按自然年批量出单，保持服务周期、房间口径与唯一约束一致。"
          action={<span className="tag">UNIQUE(room_id, fee_type, period_year)</span>}
        >
          <form className="grid gap-5" onSubmit={handlePropertyGenerate}>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">小区 ID</span>
                <input
                  className="input"
                  type="number"
                  min={1}
                  value={propertyForm.communityId}
                  onChange={(event) => setPropertyForm((current) => ({ ...current, communityId: Number(event.target.value) }))}
                />
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

            <div className="glass-soft rounded-[24px] p-5 text-sm leading-7 text-slate-600">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                <ReceiptText className="h-4 w-4 text-primary-700" />
                开单前检查
              </div>
              <ul className="mt-3 list-disc space-y-1 pl-5">
                <li>确保年费规则、面积单价与服务周期已配置完成。</li>
                <li>保持数据库唯一约束兜底，避免同房间同年重复生成有效物业费账单。</li>
                <li>已支付账单不直接覆盖，异常修正应走作废或差额补收流程。</li>
              </ul>
            </div>

            {propertyError ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{propertyError}</div> : null}
            {propertyResult ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{propertyResult}</div> : null}

            <button type="submit" className="btn-primary w-full sm:w-auto" disabled={propertyLoading}>
              {propertyLoading ? '开单中...' : '生成年度物业费账单'}
            </button>
          </form>
        </PageSection>

        <PageSection
          title="水费补出账"
          description="默认生产流程仍是抄表录入即出账；此处用于补齐指定账期的水费批量账单。"
          action={<span className="tag">建议优先走抄表入口</span>}
        >
          <form className="grid gap-5" onSubmit={handleWaterGenerate}>
            <div className="grid gap-4 md:grid-cols-3">
              <label className="block md:col-span-1">
                <span className="mb-2 block text-sm font-medium text-slate-700">小区 ID</span>
                <input
                  className="input"
                  type="number"
                  min={1}
                  value={waterForm.communityId}
                  onChange={(event) => setWaterForm((current) => ({ ...current, communityId: Number(event.target.value) }))}
                />
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

            <div className="glass-soft rounded-[24px] p-5 text-sm leading-7 text-slate-600">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                <CalendarRange className="h-4 w-4 text-primary-700" />
                月度水费说明
              </div>
              <ul className="mt-3 list-disc space-y-1 pl-5">
                <li>同一房间同一自然月只能有一条有效抄表记录和一张有效水费账单。</li>
                <li>如需真实生产闭环，请优先从“月度水费抄表”页录入读数并立即出账。</li>
                <li>本入口更适合规则调整后的补跑与运营补齐。</li>
              </ul>
            </div>

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
