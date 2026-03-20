import { useEffect, useState } from 'react'
import { BellRing, RefreshCcw } from 'lucide-react'

import { getDunningLogs, getDunningTasks, triggerDunning } from '@/api/dunning'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { DunningLog, DunningTask } from '@/types/dunning'
import { formatDate, formatDateTime } from '@/utils/format'

export default function DunningPage() {
  const [tasks, setTasks] = useState<DunningTask[]>([])
  const [logs, setLogs] = useState<DunningLog[]>([])
  const [selectedBillId, setSelectedBillId] = useState<number | undefined>()
  const [triggerDate, setTriggerDate] = useState('')
  const [loading, setLoading] = useState(false)
  const [triggerLoading, setTriggerLoading] = useState(false)
  const [logLoading, setLogLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function loadTasks() {
    setLoading(true)
    setError('')
    try {
      setTasks(await getDunningTasks())
    } catch (err) {
      setError(err instanceof Error ? err.message : '催缴任务加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadTasks()
  }, [])

  async function handleTrigger() {
    setTriggerLoading(true)
    setError('')
    setMessage('')
    try {
      const result = await triggerDunning({ triggerDate: triggerDate || undefined })
      setTasks(result)
      setMessage(`催缴触发完成，当前返回 ${result.length} 条任务；仅包含逾期未支付账单生成的任务。`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '催缴触发失败')
    } finally {
      setTriggerLoading(false)
    }
  }

  async function handleLoadLogs(billId: number) {
    setSelectedBillId(billId)
    setLogLoading(true)
    setError('')
    try {
      setLogs(await getDunningLogs(billId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '催缴日志加载失败')
    } finally {
      setLogLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div>
          <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">运营协同</div>
          <h1 className="mt-2 text-2xl font-semibold text-slate-950">催缴任务</h1>
        </div>
      </section>

      {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <PageSection title="手动触发" description="按日期触发。">
          <div className="flex flex-wrap gap-3">
            <input className="input w-48" type="date" value={triggerDate} onChange={(event) => setTriggerDate(event.target.value)} />
            <button type="button" className="btn-primary gap-2" onClick={() => void handleTrigger()} disabled={triggerLoading}>
              <BellRing className="h-4 w-4" />
              {triggerLoading ? '触发中...' : '立即触发'}
            </button>
            <button type="button" className="btn-secondary gap-2" onClick={() => void loadTasks()} disabled={loading}>
              <RefreshCcw className="h-4 w-4" />
              刷新任务
            </button>
          </div>
        </PageSection>

        <PageSection title="发送日志" description={selectedBillId ? `账单 ${selectedBillId}` : '选择任务后查看。'}>
          <AsyncState loading={logLoading} empty={!logs.length} emptyDescription="暂无催缴日志。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">任务 ID</th>
                    <th className="px-4 py-3 font-medium">发送渠道</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">发送内容</th>
                    <th className="px-4 py-3 font-medium">发送时间</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 text-slate-600">{item.taskId}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.sendChannel} /></td>
                      <td className="px-4 py-4"><StatusBadge value={item.status} /></td>
                      <td className="px-4 py-4 text-slate-600">{item.content || '--'}</td>
                      <td className="px-4 py-4 text-slate-600">{formatDateTime(item.sentAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>
      </div>

      <PageSection title="催缴任务列表" description="查看任务与状态。">
        <AsyncState loading={loading} error={error} empty={!tasks.length} emptyDescription="暂无催缴任务。">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-4 py-3 font-medium">任务编号</th>
                  <th className="px-4 py-3 font-medium">账单 ID</th>
                  <th className="px-4 py-3 font-medium">用户组 ID</th>
                  <th className="px-4 py-3 font-medium">组织 ID</th>
                  <th className="px-4 py-3 font-medium">租户编码</th>
                  <th className="px-4 py-3 font-medium">任务说明</th>
                  <th className="px-4 py-3 font-medium">触发方式</th>
                  <th className="px-4 py-3 font-medium">触发日期</th>
                  <th className="px-4 py-3 font-medium">状态</th>
                  <th className="px-4 py-3 font-medium">执行时间</th>
                  <th className="px-4 py-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="px-4 py-4 font-medium text-slate-900">{item.taskNo}</td>
                    <td className="px-4 py-4 text-slate-600">{item.billId}</td>
                    <td className="px-4 py-4 text-slate-600">{item.groupId || '--'}</td>
                    <td className="px-4 py-4 text-slate-600">{item.orgUnitId || '--'}</td>
                    <td className="px-4 py-4 text-slate-600">{item.tenantCode || '--'}</td>
                    <td className="px-4 py-4 text-slate-600">{item.remark || '--'}</td>
                    <td className="px-4 py-4"><StatusBadge value={item.triggerType} /></td>
                    <td className="px-4 py-4 text-slate-600">{formatDate(item.triggerDate)}</td>
                    <td className="px-4 py-4"><StatusBadge value={item.status} /></td>
                    <td className="px-4 py-4 text-slate-600">{formatDateTime(item.executedAt)}</td>
                    <td className="px-4 py-4"><button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={() => void handleLoadLogs(item.billId)}>查看日志</button></td>
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
