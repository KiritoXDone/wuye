import { useState } from 'react'
import { Download, FileSearch, Upload } from 'lucide-react'

import { createBillExport, createBillImport, getExportJob, getImportBatch, getImportBatchErrors } from '@/api/import-export'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { ExportJob, ImportBatch, ImportRowError } from '@/types/import-export'
import { formatDateTime } from '@/utils/format'

const now = new Date()

export default function ImportExportPage() {
  const [importForm, setImportForm] = useState({ fileUrl: 'https://example.com/imports/bills-2026-03.xlsx' })
  const [exportForm, setExportForm] = useState({
    periodYear: now.getFullYear(),
    periodMonth: now.getMonth() + 1,
    feeType: 'PROPERTY',
    status: 'ISSUED',
  })
  const [query, setQuery] = useState<{ importBatchId?: number; exportJobId?: number }>({})
  const [importBatch, setImportBatch] = useState<ImportBatch | null>(null)
  const [exportJob, setExportJob] = useState<ExportJob | null>(null)
  const [importErrors, setImportErrors] = useState<ImportRowError[]>([])
  const [importLoading, setImportLoading] = useState(false)
  const [importQueryLoading, setImportQueryLoading] = useState(false)
  const [importErrorLoading, setImportErrorLoading] = useState(false)
  const [exportLoading, setExportLoading] = useState(false)
  const [exportQueryLoading, setExportQueryLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  async function handleCreateImport() {
    if (!importForm.fileUrl.trim()) {
      setError('请输入导入文件地址。')
      return
    }
    setImportLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await createBillImport(importForm)
      setImportBatch(result)
      setQuery((current) => ({ ...current, importBatchId: result.id }))
      setImportErrors([])
      setMessage(`导入批次 ${result.batchNo} 已创建，当前状态 ${result.status}。`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建导入批次失败')
    } finally {
      setImportLoading(false)
    }
  }

  async function handleQueryImport() {
    if (!query.importBatchId) {
      setError('请先输入导入批次 ID。')
      return
    }
    setImportQueryLoading(true)
    setError('')
    try {
      setImportBatch(await getImportBatch(query.importBatchId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '查询导入批次失败')
    } finally {
      setImportQueryLoading(false)
    }
  }

  async function handleLoadImportErrors() {
    if (!query.importBatchId) {
      setError('请先输入导入批次 ID。')
      return
    }
    setImportErrorLoading(true)
    setError('')
    try {
      setImportErrors(await getImportBatchErrors(query.importBatchId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载错误行失败')
    } finally {
      setImportErrorLoading(false)
    }
  }

  async function handleCreateExport() {
    setExportLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await createBillExport({
        periodYear: Number(exportForm.periodYear),
        periodMonth: Number(exportForm.periodMonth),
        feeType: exportForm.feeType || undefined,
        status: exportForm.status || undefined,
      })
      setExportJob(result)
      setQuery((current) => ({ ...current, exportJobId: result.id }))
      setMessage(`导出任务 ${result.id} 已创建，当前状态 ${result.status}。`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建导出任务失败')
    } finally {
      setExportLoading(false)
    }
  }

  async function handleQueryExport() {
    if (!query.exportJobId) {
      setError('请先输入导出任务 ID。')
      return
    }
    setExportQueryLoading(true)
    setError('')
    try {
      setExportJob(await getExportJob(query.exportJobId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '查询导出任务失败')
    } finally {
      setExportQueryLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div>
          <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">导入导出</div>
          <h1 className="mt-2 text-2xl font-semibold text-slate-950">任务中心</h1>
        </div>
        {error ? <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}
      </section>

      <div className="grid gap-6 xl:grid-cols-2">
        <PageSection title="账单导入" description="创建批次并查看结果。">
          <div className="grid gap-4">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">导入文件地址</span>
              <input className="input" value={importForm.fileUrl} onChange={(event) => setImportForm({ fileUrl: event.target.value })} placeholder="请输入对象存储或可访问文件 URL" />
            </label>
            <div className="flex flex-wrap gap-2">
              <button type="button" className="btn-primary gap-2" onClick={() => void handleCreateImport()} disabled={importLoading}>
                <Upload className="h-4 w-4" />
                {importLoading ? '创建中...' : '创建导入批次'}
              </button>
              <input className="input w-32" type="number" min={1} value={query.importBatchId || ''} onChange={(event) => setQuery((current) => ({ ...current, importBatchId: event.target.value ? Number(event.target.value) : undefined }))} placeholder="批次 ID" />
              <button type="button" className="btn-secondary" onClick={() => void handleQueryImport()} disabled={importQueryLoading}>查询批次</button>
              <button type="button" className="btn-secondary" onClick={() => void handleLoadImportErrors()} disabled={importErrorLoading}>查看错误行</button>
            </div>
          </div>

          <div className="mt-6 space-y-4">
            <AsyncState loading={importQueryLoading} empty={!importBatch} emptyDescription="创建或查询后可查看导入结果。">
              {importBatch ? (
                <div className="glass-soft rounded-[24px] p-5 text-sm">
                  <div className="flex items-center justify-between gap-3"><span className="text-slate-500">批次号</span><span className="font-medium text-slate-900">{importBatch.batchNo}</span></div>
                  <div className="mt-3 flex items-center justify-between gap-3"><span className="text-slate-500">状态</span><StatusBadge value={importBatch.status} /></div>
                  <div className="mt-3 flex items-center justify-between gap-3"><span className="text-slate-500">总行数</span><span className="font-medium text-slate-900">{importBatch.totalCount}</span></div>
                  <div className="mt-3 flex items-center justify-between gap-3"><span className="text-slate-500">成功 / 失败</span><span className="font-medium text-slate-900">{importBatch.successCount} / {importBatch.failCount}</span></div>
                </div>
              ) : null}
            </AsyncState>

            <AsyncState loading={importErrorLoading} empty={!importErrors.length} emptyDescription="当前批次暂无错误行。">
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-slate-500">
                      <th className="px-4 py-3 font-medium">行号</th>
                      <th className="px-4 py-3 font-medium">错误码</th>
                      <th className="px-4 py-3 font-medium">错误信息</th>
                      <th className="px-4 py-3 font-medium">原始数据</th>
                    </tr>
                  </thead>
                  <tbody>
                    {importErrors.map((item) => (
                      <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                        <td className="px-4 py-4 text-slate-600">{item.rowNo}</td>
                        <td className="px-4 py-4"><StatusBadge value={item.errorCode} /></td>
                        <td className="px-4 py-4 text-slate-600">{item.errorMessage}</td>
                        <td className="px-4 py-4 text-slate-500">{item.rawData}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AsyncState>
          </div>
        </PageSection>

        <PageSection title="账单导出" description="创建并查询导出任务。">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">年份</span>
              <input className="input" type="number" min={2020} max={2100} value={exportForm.periodYear} onChange={(event) => setExportForm((current) => ({ ...current, periodYear: Number(event.target.value) }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">月份</span>
              <select className="input" value={exportForm.periodMonth} onChange={(event) => setExportForm((current) => ({ ...current, periodMonth: Number(event.target.value) }))}>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>{month} 月</option>
                ))}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">费用类型</span>
              <select className="input" value={exportForm.feeType} onChange={(event) => setExportForm((current) => ({ ...current, feeType: event.target.value }))}>
                <option value="PROPERTY">物业费</option>
                <option value="WATER">水费</option>
                <option value="">全部</option>
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">账单状态</span>
              <select className="input" value={exportForm.status} onChange={(event) => setExportForm((current) => ({ ...current, status: event.target.value }))}>
                <option value="ISSUED">已出账</option>
                <option value="PAID">已支付</option>
                <option value="CANCELLED">已取消</option>
                <option value="">全部</option>
              </select>
            </label>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <button type="button" className="btn-primary gap-2" onClick={() => void handleCreateExport()} disabled={exportLoading}>
              <Download className="h-4 w-4" />
              {exportLoading ? '创建中...' : '创建导出任务'}
            </button>
            <input className="input w-32" type="number" min={1} value={query.exportJobId || ''} onChange={(event) => setQuery((current) => ({ ...current, exportJobId: event.target.value ? Number(event.target.value) : undefined }))} placeholder="任务 ID" />
            <button type="button" className="btn-secondary gap-2" onClick={() => void handleQueryExport()} disabled={exportQueryLoading}>
              <FileSearch className="h-4 w-4" />
              查询任务
            </button>
          </div>

          <div className="mt-6">
            <AsyncState loading={exportQueryLoading} empty={!exportJob} emptyDescription="创建或查询后可查看导出任务详情。">
              {exportJob ? (
                <div className="glass-soft rounded-[24px] p-5 text-sm">
                  <div className="flex items-center justify-between gap-3"><span className="text-slate-500">任务 ID</span><span className="font-medium text-slate-900">{exportJob.id}</span></div>
                  <div className="mt-3 flex items-center justify-between gap-3"><span className="text-slate-500">状态</span><StatusBadge value={exportJob.status} /></div>
                  <div className="mt-3 flex items-center justify-between gap-3"><span className="text-slate-500">导出类型</span><span className="font-medium text-slate-900">{exportJob.exportType}</span></div>
                  <div className="mt-3 flex items-center justify-between gap-3"><span className="text-slate-500">过期时间</span><span className="font-medium text-slate-900">{formatDateTime(exportJob.expiredAt)}</span></div>
                  <div className="mt-3 break-all text-slate-600"><span className="text-slate-500">文件地址：</span>{exportJob.fileUrl || '--'}</div>
                  <div className="mt-3 break-all text-slate-500"><span className="text-slate-500">请求快照：</span>{exportJob.requestJson || '--'}</div>
                </div>
              ) : null}
            </AsyncState>
          </div>
        </PageSection>
      </div>
    </div>
  )
}
