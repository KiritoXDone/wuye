import { useState } from 'react'

import { processInvoiceApplication } from '@/api/invoices'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { invoiceStatusOptions } from '@/constants/options'
import type { InvoiceApplication } from '@/types/invoice'
import { formatDateTime } from '@/utils/format'

export default function InvoiceApplicationsPage() {
  const [form, setForm] = useState({ applicationId: '', status: 'APPROVED', remark: '已开具电子发票' })
  const [result, setResult] = useState<InvoiceApplication | null>(null)
  const [processedHistory, setProcessedHistory] = useState<InvoiceApplication[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function handleProcess() {
    if (!form.applicationId) {
      setError('请输入发票申请 ID。')
      return
    }
    setLoading(true)
    setError('')
    setMessage('')
    try {
      const current = await processInvoiceApplication(Number(form.applicationId), {
        status: form.status,
        remark: form.remark || undefined,
      })
      setResult(current)
      setProcessedHistory((history) => [current, ...history.filter((item) => item.id !== current.id)])
      setMessage('发票申请处理成功。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '发票申请处理失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div>
          <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">运营协同</div>
          <h1 className="mt-2 text-2xl font-semibold text-slate-950">发票申请</h1>
        </div>
      </section>

      {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <PageSection title="处理申请" description="按申请 ID 处理。">
          <div className="mt-4 grid gap-4">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">申请 ID</span>
              <input className="input" type="number" min={1} value={form.applicationId} onChange={(event) => setForm((current) => ({ ...current, applicationId: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">处理状态</span>
              <select className="input" value={form.status} onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}>
                {invoiceStatusOptions.filter((item) => item.value !== 'APPLIED').map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">处理备注</span>
              <textarea className="textarea" rows={4} value={form.remark} onChange={(event) => setForm((current) => ({ ...current, remark: event.target.value }))} />
            </label>
            <button type="button" className="btn-primary w-full" onClick={() => void handleProcess()} disabled={loading}>
              {loading ? '提交中...' : '提交处理'}
            </button>
          </div>
        </PageSection>

        <PageSection title="接口边界" description="当前页仅提供处理入口。">
          <div className="space-y-3 text-sm text-slate-600">
            <div className="rounded-2xl border border-slate-200 bg-white/75 px-4 py-4"><span className="font-medium text-slate-900">住户侧列表：</span>GET /api/v1/me/invoices/applications</div>
            <div className="rounded-2xl border border-slate-200 bg-white/75 px-4 py-4"><span className="font-medium text-slate-900">后台处理：</span>POST /api/v1/admin/invoices/applications/{'{applicationId}'}/process</div>
            <div className="rounded-2xl border border-slate-200 bg-white/75 px-4 py-4"><span className="font-medium text-slate-900">电子凭证入口：</span>住户支付结果页 / 已支付账单详情</div>
          </div>
        </PageSection>
      </div>

      <PageSection title="处理结果" description="处理成功后展示后端返回的最新申请状态，便于联调确认。">
        <AsyncState empty={!result} emptyDescription="提交处理后可在这里查看返回结果。">
          {result ? (
            <div className="glass-soft rounded-[24px] p-5 text-sm">
              <div className="grid gap-3 md:grid-cols-2">
                <div><span className="text-slate-500">申请单号：</span><span className="font-medium text-slate-900">{result.applicationNo}</span></div>
                <div><span className="text-slate-500">状态：</span><StatusBadge value={result.status} /></div>
                <div><span className="text-slate-500">账单 ID：</span><span className="font-medium text-slate-900">{result.billId}</span></div>
                <div><span className="text-slate-500">支付单号：</span><span className="font-medium text-slate-900">{result.payOrderNo}</span></div>
                <div><span className="text-slate-500">抬头：</span><span className="font-medium text-slate-900">{result.invoiceTitle}</span></div>
                <div><span className="text-slate-500">税号：</span><span className="font-medium text-slate-900">{result.taxNo || '--'}</span></div>
                <div><span className="text-slate-500">申请时间：</span><span className="font-medium text-slate-900">{formatDateTime(result.appliedAt)}</span></div>
                <div><span className="text-slate-500">处理时间：</span><span className="font-medium text-slate-900">{formatDateTime(result.processedAt)}</span></div>
              </div>
              <div className="mt-3 text-slate-600"><span className="text-slate-500">备注：</span>{result.remark || '--'}</div>
            </div>
          ) : null}
        </AsyncState>
      </PageSection>

      <PageSection title="本次会话已处理记录" description="仅展示当前页面会话内成功处理的记录，刷新页面后会清空。">
        <AsyncState empty={!processedHistory.length} emptyDescription="当前会话尚未处理任何申请。">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-4 py-3 font-medium">申请单号</th>
                  <th className="px-4 py-3 font-medium">账单 ID</th>
                  <th className="px-4 py-3 font-medium">支付单号</th>
                  <th className="px-4 py-3 font-medium">状态</th>
                  <th className="px-4 py-3 font-medium">发票抬头</th>
                  <th className="px-4 py-3 font-medium">处理时间</th>
                </tr>
              </thead>
              <tbody>
                {processedHistory.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="px-4 py-4 font-medium text-slate-900">{item.applicationNo}</td>
                    <td className="px-4 py-4 text-slate-600">{item.billId}</td>
                    <td className="px-4 py-4 text-slate-600">{item.payOrderNo}</td>
                    <td className="px-4 py-4"><StatusBadge value={item.status} /></td>
                    <td className="px-4 py-4 text-slate-600">{item.invoiceTitle}</td>
                    <td className="px-4 py-4 text-slate-600">{formatDateTime(item.processedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </AsyncState>
      </PageSection>
    </div>
  )
}
