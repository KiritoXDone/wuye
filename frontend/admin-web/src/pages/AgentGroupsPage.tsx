import { useEffect, useState } from 'react'
import { Shield, UserPlus } from 'lucide-react'

import { createAgentGroup, getAgentGroups } from '@/api/agent-groups'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import { enabledStatusOptions, permissionOptions } from '@/constants/options'
import type { AgentGroup, AgentGroupCreatePayload } from '@/types/agent-group'

export default function AgentGroupsPage() {
  const [list, setList] = useState<AgentGroup[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<AgentGroupCreatePayload>({
    agentCode: 'AGENT-A',
    groupCode: 'G-COMM001-1-2',
    permission: 'VIEW',
    status: 1,
  })

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      setList(await getAgentGroups())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Agent 授权列表加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  async function handleCreate() {
    if (!form.agentCode.trim() || !form.groupCode.trim()) {
      setError('请输入 Agent 编码和用户组编码。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      await createAgentGroup(form)
      setMessage('Agent 授权保存成功。')
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存 Agent 授权失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="glass-panel overflow-hidden p-6 sm:p-7">
        <div>
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">治理与授权</div>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">维护 Agent 与用户组的授权关系，核对数据范围最小闭环</h1>
          <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
            当前列表接口返回的是组维度信息，适合核对用户组与组织归属；新增授权时仅提交 agentCode、groupCode、permission、status。
          </p>
        </div>
      </section>

      {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.9fr]">
        <PageSection title="授权列表" description="按当前后端返回字段展示用户组与权限，用于核对 Agent 数据范围配置。">
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无授权记录。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">组 ID</th>
                    <th className="px-4 py-3 font-medium">组编码</th>
                    <th className="px-4 py-3 font-medium">组名称</th>
                    <th className="px-4 py-3 font-medium">组织单元</th>
                    <th className="px-4 py-3 font-medium">租户编码</th>
                    <th className="px-4 py-3 font-medium">权限</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={`${item.groupId}-${item.groupCode}`} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.groupId}</td>
                      <td className="px-4 py-4 text-slate-600">{item.groupCode}</td>
                      <td className="px-4 py-4 text-slate-900">{item.groupName}</td>
                      <td className="px-4 py-4 text-slate-600">{item.orgUnitName || '--'}</td>
                      <td className="px-4 py-4 text-slate-600">{item.tenantCode || '--'}</td>
                      <td className="px-4 py-4"><StatusBadge value={item.permission} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>

        <PageSection title="新增授权" description="保持与后端 AgentGroupAssignDTO 一致，只提交最小授权字段。">
          <div className="grid gap-4">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Agent 编码</span>
              <input className="input" value={form.agentCode} onChange={(event) => setForm((current) => ({ ...current, agentCode: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">用户组编码</span>
              <input className="input" value={form.groupCode} onChange={(event) => setForm((current) => ({ ...current, groupCode: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">权限</span>
              <select className="input" value={form.permission} onChange={(event) => setForm((current) => ({ ...current, permission: event.target.value }))}>
                {permissionOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">状态</span>
              <select className="input" value={form.status} onChange={(event) => setForm((current) => ({ ...current, status: Number(event.target.value) }))}>
                {enabledStatusOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </label>
            <button type="button" className="btn-primary w-full gap-2" onClick={() => void handleCreate()} disabled={submitLoading}>
              <UserPlus className="h-4 w-4" />
              {submitLoading ? '保存中...' : '保存授权'}
            </button>
          </div>
        </PageSection>
      </div>
    </div>
  )
}
