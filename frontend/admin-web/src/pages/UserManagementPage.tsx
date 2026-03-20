import { useEffect, useMemo, useState } from 'react'
import { RefreshCcw, UserPlus } from 'lucide-react'

import { createAdminUser, getAdminUsers, resetAdminUserPassword, updateAdminUserStatus } from '@/api/users'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { AdminUser, AdminUserCreatePayload, AdminUserListQuery } from '@/types/user'
import { formatDateTime } from '@/utils/format'

const initialForm: AdminUserCreatePayload = {
  username: '',
  password: '',
  realName: '',
  mobile: '',
}

const initialFilters: AdminUserListQuery = {
  accountType: '',
}

export default function UserManagementPage() {
  const [list, setList] = useState<AdminUser[]>([])
  const [filters, setFilters] = useState<AdminUserListQuery>(initialFilters)
  const [loading, setLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [resettingId, setResettingId] = useState<number | null>(null)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [form, setForm] = useState<AdminUserCreatePayload>(initialForm)

  const adminCount = useMemo(() => list.filter((item) => item.accountType === 'ADMIN').length, [list])
  const residentCount = useMemo(() => list.filter((item) => item.accountType === 'RESIDENT').length, [list])

  async function loadData(nextFilters = filters) {
    setLoading(true)
    setError('')
    try {
      setList(await getAdminUsers(nextFilters.accountType ? nextFilters : undefined))
    } catch (err) {
      setError(err instanceof Error ? err.message : '账户列表加载失败')
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
      setMessage(item.status === 1 ? '账户已停用。' : '账户已启用。')
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
        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div className="space-y-3">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">账户治理</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">用户管理</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">收口后台账户停启用、筛选与密码重置，减少页面分块和操作换行。</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">账户总数</div>
                <div className="mt-2 text-2xl font-semibold text-slate-950">{list.length}</div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">管理员</div>
                <div className="mt-2 text-2xl font-semibold text-slate-950">{adminCount}</div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">居民</div>
                <div className="mt-2 text-2xl font-semibold text-slate-950">{residentCount}</div>
              </div>
            </div>
          </div>
          <div className="flex flex-wrap gap-2 xl:justify-end">
            <button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={() => void loadData()} disabled={loading}>
              <RefreshCcw className="h-4 w-4" />
              {loading ? '刷新中...' : '刷新'}
            </button>
            <button type="button" className="btn-primary gap-2 whitespace-nowrap" onClick={() => setShowCreateForm((current) => !current)}>
              <UserPlus className="h-4 w-4" />
              {showCreateForm ? '收起新增' : '新增管理员'}
            </button>
          </div>
        </div>
      </section>

      {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      {showCreateForm ? (
        <PageSection title="新增管理员" description="仅管理员支持在此创建后台管理员账户。">
          <div className="grid gap-4 lg:grid-cols-[repeat(4,minmax(0,1fr))_auto] lg:items-end">
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
            <button type="button" className="btn-primary min-h-11 gap-2 whitespace-nowrap lg:px-5" onClick={() => void handleCreate()} disabled={submitLoading}>
              <UserPlus className="h-4 w-4" />
              {submitLoading ? '创建中...' : '确认新增'}
            </button>
          </div>
        </PageSection>
      ) : null}

      <PageSection
        title="账户列表"
        description="查看管理员与居民账户，支持按账户类型筛选。"
        action={
          <div className="flex flex-wrap gap-2">
            <select className="input min-w-[148px]" value={filters.accountType || ''} onChange={(event) => setFilters({ accountType: event.target.value })}>
              <option value="">全部账户</option>
              <option value="ADMIN">管理员</option>
              <option value="RESIDENT">居民</option>
            </select>
            <button type="button" className="btn-secondary whitespace-nowrap" onClick={() => void loadData(filters)} disabled={loading}>查询</button>
          </div>
        }
      >
        <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前条件下暂无账户。">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="whitespace-nowrap px-4 py-3 font-medium">账户编号</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">类型</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">用户名</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">姓名</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">手机号</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">最近登录</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="whitespace-nowrap px-4 py-4 font-medium text-slate-900">{item.accountNo}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{item.accountType}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{item.username || '--'}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-900">{item.realName}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{item.mobile || '--'}</td>
                    <td className="whitespace-nowrap px-4 py-4"><StatusBadge value={String(item.status)} /></td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatDateTime(item.lastLoginAt)}</td>
                    <td className="px-4 py-4">
                      <div className="flex flex-wrap gap-2 xl:flex-nowrap">
                        <button
                          type="button"
                          className="btn-secondary min-h-10 whitespace-nowrap px-3 py-2"
                          onClick={() => void handleToggleStatus(item)}
                          disabled={togglingId === item.id}
                        >
                          {togglingId === item.id ? '处理中...' : item.status === 1 ? '停用账户' : '启用账户'}
                        </button>
                        {item.accountType === 'ADMIN' ? (
                          <button
                            type="button"
                            className="btn-secondary min-h-10 whitespace-nowrap px-3 py-2"
                            onClick={() => void handleResetPassword(item)}
                            disabled={resettingId === item.id}
                          >
                            {resettingId === item.id ? '重置中...' : '重置密码'}
                          </button>
                        ) : null}
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
  )
}
