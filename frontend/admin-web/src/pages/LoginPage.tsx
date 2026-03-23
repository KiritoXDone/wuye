import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { useAuthStore } from '@/stores/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useAuthStore((state) => state.login)
  const [form, setForm] = useState({ username: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const result = await login(form)
      const redirect = searchParams.get('redirect') || '/dashboard'
      navigate(redirect, { replace: true })
      if (result.needResetPassword) {
        setError('当前账号需要尽快完成密码更新，请登录后立即处理。')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6 py-10">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-sm sm:p-9">
        <div className="text-xs font-medium uppercase tracking-[0.18em] text-slate-500">物业运营后台</div>
        <h1 className="mt-3 text-3xl font-semibold text-slate-950">登录</h1>
        <p className="mt-2 text-sm leading-6 text-slate-500">使用管理员账号进入控制台。</p>

        <div className="mt-5 text-sm text-slate-500">请使用通过环境变量初始化的管理员账号登录。</div>

        <form className="mt-6 space-y-5" onSubmit={handleSubmit}>
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">用户名</span>
            <input
              className="input"
              value={form.username}
              onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
              placeholder="请输入用户名"
              autoComplete="username"
              required
            />
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">密码</span>
            <input
              className="input"
              type="password"
              value={form.password}
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              placeholder="请输入密码"
              autoComplete="current-password"
              required
            />
          </label>

          {error ? <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">{error}</div> : null}

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? '登录中...' : '登录系统'}
          </button>
        </form>
      </div>
    </div>
  )
}
