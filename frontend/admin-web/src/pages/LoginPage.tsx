import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { useAuthStore } from '@/stores/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useAuthStore((state) => state.login)
  const [form, setForm] = useState({ username: 'admin', password: '123456' })
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
        setError('当前账号需尽快完成密码更新，请登录后尽快处理。')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-screen bg-transparent lg:grid-cols-[1.15fr_460px]">
      <section className="flex flex-col justify-center px-6 py-12 sm:px-10 lg:px-16 xl:px-20">
        <span className="text-xs font-semibold uppercase tracking-[0.24em] text-primary-700">Property Operations Console</span>
        <h1 className="mt-5 max-w-3xl text-4xl font-semibold leading-tight text-slate-950 sm:text-5xl">
          面向收费、财务与运营团队的物业运营管理平台
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-8 text-slate-600">
          围绕年度物业费、月度水费、账单复核和经营看板构建统一工作台，让收费闭环、抄表录入和账务追踪保持同一口径。
        </p>

        <div className="mt-10 grid gap-4 xl:grid-cols-3">
          {[
            ['统一台账', '账单、开单、抄表一体协同', '首页、账单和抄表页共享同一业务口径，减少切换成本。'],
            ['生产口径', '年度物业费 + 月度水费', '突出服务周期、房间统计口径和录入即出账的核心流程。'],
            ['值班友好', '适合收费与财务联动复核', '聚焦高频后台操作，不再保留旧 Vue 管理台的过渡叙事。'],
          ].map(([label, title, text]) => (
            <article key={label} className="glass-panel p-6">
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">{label}</div>
              <h2 className="mt-4 text-lg font-semibold text-slate-950">{title}</h2>
              <p className="mt-3 text-sm leading-7 text-slate-600">{text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="flex items-center justify-center px-6 py-10 sm:px-10">
        <div className="panel w-full max-w-md p-8 sm:p-9">
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-700">平台登录</div>
          <h2 className="mt-3 text-3xl font-semibold text-slate-950">进入运营控制台</h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">请输入管理员账号信息，进入 React 管理端新工作台。</p>

          <div className="glass-soft mt-6 rounded-2xl px-4 py-3 text-sm text-slate-600">
            默认账号 <span className="font-semibold text-slate-900">admin</span> / <span className="font-semibold text-slate-900">123456</span>
          </div>

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

            {error ? <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">{error}</div> : null}

            <button type="submit" className="btn-primary w-full" disabled={loading}>
              {loading ? '登录中...' : '登录系统'}
            </button>
          </form>
        </div>
      </section>
    </div>
  )
}
