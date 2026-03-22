import { useEffect, useMemo, useState } from 'react'
import { Gift, RefreshCcw, TicketPercent, Users, X } from 'lucide-react'

import { getCouponExchangesByTemplate, getCoupons, manualIssueCoupon, saveCoupon, updateVoucherExchangeStatus } from '@/api/coupons'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { couponTemplateTypeOptions, couponTriggerTypeOptions, discountModeOptions, feeTypeOptions } from '@/constants/options'
import type { CouponManualIssuePayload, CouponSummary, CouponUpsertPayload, VoucherExchangeRecord } from '@/types/coupon'
import { formatDateTime, formatMoney } from '@/utils/format'

const initialForm: CouponUpsertPayload = {
  templateCode: '',
  type: 'VOUCHER',
  feeType: 'PROPERTY',
  name: '',
  discountMode: 'FIXED',
  valueAmount: 0,
  thresholdAmount: 0,
  validFrom: '2026-01-01 00:00:00',
  validTo: '2026-12-31 23:59:59',
  status: 1,
  triggerType: '',
  minPayAmount: 0,
  rewardCount: 1,
  ruleStatus: 1,
}

const initialIssueForm: CouponManualIssuePayload = {
  templateId: 0,
  ownerAccountId: 0,
  issueCount: 1,
  remark: '',
}

export default function CouponsPage() {
  const [list, setList] = useState<CouponSummary[]>([])
  const [selectedCoupon, setSelectedCoupon] = useState<CouponSummary | null>(null)
  const [selectedExchanges, setSelectedExchanges] = useState<VoucherExchangeRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [instancesLoading, setInstancesLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [issueLoading, setIssueLoading] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showIssueModal, setShowIssueModal] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<CouponUpsertPayload>(initialForm)
  const [issueForm, setIssueForm] = useState<CouponManualIssuePayload>(initialIssueForm)

  const selectedRuleHint = useMemo(() => {
    if (!form.triggerType) return '当前不自动发放，仅支持手工发放或居民主动使用。'
    if (form.triggerType === 'LOGIN') return '居民登录成功后按日幂等发放。'
    return '支付成功后按规则自动发放奖励券。'
  }, [form.triggerType])

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      setList(await getCoupons())
    } catch (err) {
      setError(err instanceof Error ? err.message : '优惠券列表加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  async function openInstances(item: CouponSummary) {
    setSelectedCoupon(item)
    setInstancesLoading(true)
    setError('')
    try {
      setSelectedExchanges(await getCouponExchangesByTemplate(item.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '兑换记录加载失败')
      setSelectedExchanges([])
    } finally {
      setInstancesLoading(false)
    }
  }

  function openCreate() {
    setForm(initialForm)
    setShowEditModal(true)
  }

  function openEdit(item: CouponSummary) {
    setForm({
      id: item.id,
      templateCode: item.templateCode,
      type: item.type,
      feeType: item.feeType,
      name: item.name,
      discountMode: item.discountMode,
      valueAmount: Number(item.valueAmount),
      thresholdAmount: Number(item.thresholdAmount),
      validFrom: item.validFrom,
      validTo: item.validTo,
      status: item.status,
      triggerType: item.triggerType || '',
      minPayAmount: Number(item.minPayAmount || 0),
      rewardCount: item.rewardCount || 1,
      ruleStatus: item.ruleStatus ?? 1,
    })
    setShowEditModal(true)
  }

  function openManualIssue(item: CouponSummary) {
    setSelectedCoupon(item)
    setIssueForm({ ...initialIssueForm, templateId: item.id })
    setShowIssueModal(true)
  }

  async function handleSave() {
    if (!form.templateCode.trim() || !form.name.trim()) {
      setError('请填写优惠券编码和名称。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await saveCoupon({
        ...form,
        feeType: form.feeType || undefined,
        triggerType: form.triggerType || undefined,
        minPayAmount: form.triggerType ? Number(form.minPayAmount || 0) : undefined,
        rewardCount: form.triggerType ? Number(form.rewardCount || 1) : undefined,
        ruleStatus: form.triggerType ? (form.ruleStatus ?? 1) : undefined,
      })
      setMessage(`优惠券“${result.name}”已保存。`)
      setShowEditModal(false)
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存优惠券失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  async function handleManualIssue() {
    if (!issueForm.templateId || !issueForm.ownerAccountId || !issueForm.issueCount) {
      setError('请填写账号 ID 和发放张数。')
      return
    }
    setIssueLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await manualIssueCoupon({
        templateId: issueForm.templateId,
        ownerAccountId: Number(issueForm.ownerAccountId),
        issueCount: Number(issueForm.issueCount),
        remark: issueForm.remark?.trim() || undefined,
      })
      setMessage(`手工发券成功，共发放 ${result.issueCount} 张。`)
      setShowIssueModal(false)
      if (selectedCoupon) {
        await openInstances(selectedCoupon)
      }
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '手工发券失败')
    } finally {
      setIssueLoading(false)
    }
  }

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">券体系</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">优惠券中心</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">统一维护优惠券、自动发放规则、兑换记录，并支持手工补发。</p>
            </div>
            <div className="flex flex-wrap gap-2 xl:justify-end">
              <button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={() => void loadData()} disabled={loading}>
                <RefreshCcw className="h-4 w-4" />
                {loading ? '刷新中...' : '刷新'}
              </button>
              <button type="button" className="btn-primary gap-2 whitespace-nowrap" onClick={openCreate}>
                <TicketPercent className="h-4 w-4" />
                新建优惠券
              </button>
            </div>
          </div>
        </section>

        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <PageSection title="优惠券列表" description="从优惠券视角统一查看模板、规则和发放数量。">
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前暂无优惠券。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="whitespace-nowrap px-4 py-3 font-medium">编码</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">名称</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">类型</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">费种</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">抵扣规则</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">发放规则</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">已发放</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                    <th className="whitespace-nowrap px-4 py-3 font-medium text-right">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.templateCode}</td>
                      <td className="px-4 py-4 text-slate-900">
                        <div className="font-medium">{item.name}</div>
                        <div className="mt-1 text-xs text-slate-500">{formatDateTime(item.validFrom)} ~ {formatDateTime(item.validTo)}</div>
                      </td>
                      <td className="px-4 py-4"><StatusBadge value={item.type} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.feeType || 'ALL'} /></td>
                      <td className="px-4 py-4 text-slate-600">{formatMoney(item.valueAmount)} / 门槛 {formatMoney(item.thresholdAmount)}</td>
                      <td className="px-4 py-4 text-slate-600">
                        {item.triggerType ? (
                          <div className="space-y-1">
                            <StatusBadge value={item.triggerType} />
                            <div className="text-xs text-slate-500">{item.triggerType === 'LOGIN' ? `发放 ${item.rewardCount ?? 1} 张` : `满 ${formatMoney(item.minPayAmount || 0)} 发 ${item.rewardCount ?? 1} 张`}</div>
                          </div>
                        ) : (
                          <span className="text-slate-400">未配置</span>
                        )}
                      </td>
                      <td className="px-4 py-4 text-slate-900">{item.issuedCount}</td>
                      <td className="px-4 py-4"><StatusBadge value={String(item.status)} /></td>
                      <td className="px-4 py-4">
                        <div className="flex flex-wrap justify-end gap-2">
                          <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => void openInstances(item)}>
                            <Users className="h-4 w-4" />查看兑换
                          </button>
                          <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => openManualIssue(item)}>
                            <Gift className="h-4 w-4" />手工发放
                          </button>
                          <button type="button" className="btn-primary min-h-10 px-3 py-2" onClick={() => openEdit(item)}>编辑</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>

      </div>

      {showEditModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
          <div className="w-full max-w-4xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">{form.id ? '编辑优惠券' : '新建优惠券'}</div>
                <div className="mt-1 text-sm text-slate-500">在一个表单里统一配置优惠券基础信息和发放规则。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setShowEditModal(false)}>
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">优惠券编码</span>
                <input className="input" value={form.templateCode} onChange={(event) => setForm((current) => ({ ...current, templateCode: event.target.value }))} disabled={Boolean(form.id)} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">优惠券名称</span>
                <input className="input" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">券类型</span>
                <select className="input" value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))} disabled={Boolean(form.id)}>
                  {couponTemplateTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>) }
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
                <span className="mb-2 block text-sm font-medium text-slate-700">抵扣模式</span>
                <select className="input" value={form.discountMode} onChange={(event) => setForm((current) => ({ ...current, discountMode: event.target.value }))}>
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
                <span className="mb-2 block text-sm font-medium text-slate-700">模板状态</span>
                <select className="input" value={form.status ?? 1} onChange={(event) => setForm((current) => ({ ...current, status: Number(event.target.value) }))}>
                  <option value={1}>启用</option>
                  <option value={0}>停用</option>
                </select>
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

            <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm font-medium text-slate-900">发放规则</div>
              <div className="mt-1 text-sm text-slate-500">{selectedRuleHint}</div>
              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <label className="block lg:col-span-3">
                  <span className="mb-2 block text-sm font-medium text-slate-700">触发类型</span>
                  <select className="input" value={form.triggerType || ''} onChange={(event) => setForm((current) => ({ ...current, triggerType: event.target.value || undefined }))}>
                    {couponTriggerTypeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
                {form.triggerType && form.triggerType !== 'LOGIN' ? (
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">最低支付金额</span>
                    <input className="input" type="number" min={0} step={0.01} value={form.minPayAmount || 0} onChange={(event) => setForm((current) => ({ ...current, minPayAmount: Number(event.target.value) }))} />
                  </label>
                ) : null}
                {form.triggerType ? (
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">发放张数</span>
                    <input className="input" type="number" min={1} step={1} value={form.rewardCount || 1} onChange={(event) => setForm((current) => ({ ...current, rewardCount: Number(event.target.value) }))} />
                  </label>
                ) : null}
                {form.triggerType ? (
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">规则状态</span>
                    <select className="input" value={form.ruleStatus ?? 1} onChange={(event) => setForm((current) => ({ ...current, ruleStatus: Number(event.target.value) }))}>
                      <option value={1}>启用</option>
                      <option value={0}>停用</option>
                    </select>
                  </label>
                ) : null}
              </div>
            </div>

            <div className="mt-5 flex justify-end">
              <button type="button" className="btn-primary w-full sm:w-auto" onClick={() => void handleSave()} disabled={submitLoading}>
                {submitLoading ? '保存中...' : '保存优惠券'}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {selectedCoupon ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
          <div className="w-full max-w-5xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">兑换记录 · {selectedCoupon.name}</div>
                <div className="mt-1 text-sm text-slate-500">查看该优惠券已提交的线下自提兑换记录。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => { setSelectedCoupon(null); setSelectedExchanges([]) }}>
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4">
              <AsyncState loading={instancesLoading} error={error} empty={!selectedExchanges.length} emptyDescription="当前优惠券暂无兑换记录。">
                <div className="overflow-x-auto">
                  <table className="min-w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-slate-500">
                        <th className="whitespace-nowrap px-4 py-3 font-medium">兑换 ID</th>
                        <th className="whitespace-nowrap px-4 py-3 font-medium">用户</th>
                        <th className="whitespace-nowrap px-4 py-3 font-medium">商品</th>
                        <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                        <th className="whitespace-nowrap px-4 py-3 font-medium">自提说明</th>
                        <th className="whitespace-nowrap px-4 py-3 font-medium">提交时间</th>
                        <th className="whitespace-nowrap px-4 py-3 font-medium text-right">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedExchanges.map((item) => (
                        <tr key={item.exchangeId} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                          <td className="px-4 py-4 font-medium text-slate-900">{item.exchangeId}</td>
                          <td className="px-4 py-4 text-slate-600">{item.ownerAccountName || '--'}（ID: {item.ownerAccountId}）</td>
                          <td className="px-4 py-4 text-slate-600">{item.goodsName}{item.goodsSpec ? ` / ${item.goodsSpec}` : ''}</td>
                          <td className="px-4 py-4"><StatusBadge value={item.exchangeStatus} /></td>
                          <td className="px-4 py-4 text-slate-600">{item.pickupSite || '--'}</td>
                          <td className="px-4 py-4 text-slate-600">{formatDateTime(item.createdAt)}</td>
                          <td className="px-4 py-4">
                            <div className="flex flex-wrap justify-end gap-2">
                              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={async () => { await updateVoucherExchangeStatus(item.exchangeId, { exchangeStatus: 'FULFILLED' }); await openInstances(selectedCoupon) }}>标记完成</button>
                              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={async () => { await updateVoucherExchangeStatus(item.exchangeId, { exchangeStatus: 'CANCELLED' }); await openInstances(selectedCoupon) }}>取消兑换</button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </AsyncState>
            </div>
          </div>
        </div>
      ) : null}

      {showIssueModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
          <div className="w-full max-w-2xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-slate-950">手工发放 · {selectedCoupon?.name}</div>
                <div className="mt-1 text-sm text-slate-500">针对当前优惠券模板补发到指定账号。</div>
              </div>
              <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => setShowIssueModal(false)}>
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">目标账号 ID</span>
                <input className="input" type="number" min={1} value={issueForm.ownerAccountId || ''} onChange={(event) => setIssueForm((current) => ({ ...current, ownerAccountId: Number(event.target.value) }))} placeholder="例如 1001" />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">发放张数</span>
                <input className="input" type="number" min={1} step={1} value={issueForm.issueCount} onChange={(event) => setIssueForm((current) => ({ ...current, issueCount: Number(event.target.value) }))} />
              </label>
              <label className="block lg:col-span-2">
                <span className="mb-2 block text-sm font-medium text-slate-700">备注</span>
                <textarea className="textarea" rows={4} value={issueForm.remark || ''} onChange={(event) => setIssueForm((current) => ({ ...current, remark: event.target.value }))} placeholder="可填写补发原因、工单号等。" />
              </label>
            </div>
            <div className="mt-5 flex justify-end">
              <button type="button" className="btn-primary w-full sm:w-auto" onClick={() => void handleManualIssue()} disabled={issueLoading}>
                {issueLoading ? '发放中...' : '确认发放'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
