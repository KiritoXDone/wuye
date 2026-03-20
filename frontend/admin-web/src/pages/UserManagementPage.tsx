import { useEffect, useState } from 'react'
import { RefreshCcw, UserPlus } from 'lucide-react'

import { createAdminUser, getAdminUsers, resetAdminUserPassword, updateAdminUserStatus } from '@/api/users'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { AdminUser, AdminUserCreatePayload } from '@/types/user'
import { formatDateTime } from '@/utils/format'

const initialForm: AdminUserCreatePayload = {
  username: '',
  password: '',
  realName: '',
  mobile: '',
}

export default function UserManagementPage() {
  const [list, setList] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [resettingId, setResettingId] = useState<number | null>(null)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState<AdminUserCreatePayload>(initialForm)

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      setList(await getAdminUsers())
    } catch (err) {
      setError(err instanceof Error ? err.message : '管理员列表加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  async function handleCreate() {
    if (!form.username.trim() || !form.password.trim() || !form.realName.trim()) {
      setError('请填写用户名、密码和姓名。')
      return
    }
    setSubmitLoading(true)
    setError('')
    setMessage('')
    try {
      await createAdminUser({
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim(),
        mobile: form.mobile?.trim() || undefined,
      })
      setMessage('管理员创建成功。')
      setForm(initialForm)
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '管理员创建失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  async function handleToggleStatus(item: AdminUser) {
    setTogglingId(item.id)
    setError('')
    setMessage('')
    try {
      await updateAdminUserStatus(item.id, { status: item.status === 1 ? '0' : '1' })
      setMessage(item.status === 1 ? '管理员已停用。' : '管理员已启用。')
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : '状态更新失败')
    } finally {
      setTogglingId(null)
    }
  }

  async function handleResetPassword(item: AdminUser) {
    const nextPassword = window.prompt(`请输入 ${item.realName} 的新密码`, 'Admin@1234')
    if (!nextPassword) {
      return
    }
    setResettingId(item.id)
    setError('')
    setMessage('')
    try {
      await resetAdminUserPassword(item.id, { newPassword: nextPassword })
      setMessage('密码已重置。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '密码重置失败')
    } finally {
      setResettingId(null)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">账户治理</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">用户管理</h1>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>
      </section>

      {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1.3fr_0.9fr]">
        <PageSection title="管理员列表" description="维护后台管理员账户。">
          <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无管理员账户。">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">账户编号</th>
                    <th className="px-4 py-3 font-medium">用户名</th>
                    <th className="px-4 py-3 font-medium">姓名</th>
                    <th className="px-4 py-3 font-medium">手机号</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">最近登录</th>
                    <th className="px-4 py-3 font-medium">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.accountNo}</td>
                      <td className="px-4 py-4 text-slate-600">{item.username}</td>
                      <td className="px-4 py-4 text-slate-900">{item.realName}</td>
                      <td className="px-4 py-4 text-slate-600">{item.mobile || '--'}</td>
                      <td className="px-4 py-4"><StatusBadge value={String(item.status)} /></td>
                      <td className="px-4 py-4 text-slate-600">{formatDateTime(item.lastLoginAt)}</td>
                      <td className="px-4 py-4">
                        <div className="flex flex-wrap gap-2">
                          <button
                            type="button"
                            className="btn-secondary min-h-10 px-3 py-2"
                            onClick={() => void handleToggleStatus(item)}
                            disabled={togglingId === item.id}
                          >
                            {togglingId === item.id ? '处理中...' : item.status === 1 ? '停用' : '启用'}
                          </button>
                          <button
                            type="button"
                            className="btn-secondary min-h-10 px-3 py-2"
                            onClick={() => void handleResetPassword(item)}
                            disabled={resettingId === item.id}
                          >
                            {resettingId === item.id ? '重置中...' : '重置密码'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>

        <PageSection title="新增管理员" description="创建新的后台管理员账户。">
          <div className="grid gap-4">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">用户名</span>
              <input className="input" value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">姓名</span>
              <input className="input" value={form.realName} onChange={(event) => setForm((current) => ({ ...current, realName: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">手机号</span>
              <input className="input" value={form.mobile || ''} onChange={(event) => setForm((current) => ({ ...current, mobile: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">初始密码</span>
              <input className="input" type="password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} />
            </label>
            <button type="button" className="btn-primary w-full gap-2" onClick={() => void handleCreate()} disabled={submitLoading}>
              <UserPlus className="h-4 w-4" />
              {submitLoading ? '创建中...' : '新增管理员'}
            </button>
          </div>
        </PageSection>
      </div>
    </div>
  )
}
