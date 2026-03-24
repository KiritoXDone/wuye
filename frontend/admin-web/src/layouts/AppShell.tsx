import { useState } from 'react'
import { Bot, LogOut, Monitor, Moon, Sun, X } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'

import BuiltInAgentPage from '@/pages/BuiltInAgentPage'
import { coreRoutes, getRouteMeta } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

export default function AppShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const profile = useAuthStore((state) => state.profile)
  const logout = useAuthStore((state) => state.logout)
  const currentRoute = getRouteMeta(location.pathname)
  const [agentOpen, setAgentOpen] = useState(false)
  const themeMode = useThemeStore((state) => state.mode)
  const setThemeMode = useThemeStore((state) => state.setMode)

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 dark:bg-slate-950">
      <aside className="hidden h-full w-72 shrink-0 border-r border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950 lg:flex lg:flex-col">
        <div className="flex h-full min-h-0 flex-col gap-6 overflow-y-auto px-5 py-6 text-slate-900">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">运营后台</div>
            <h1 className="mt-2 text-xl font-semibold text-slate-950">物业运营管理平台</h1>
          </div>

          <nav className="space-y-1" aria-label="主导航">
            {coreRoutes.filter((route) => route.showInNav !== false).map((route) => {
              const Icon = route.icon
              return (
                <NavLink
                  key={route.path}
                  to={route.path}
                  className={({ isActive }) =>
                    `group flex min-h-11 cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition duration-150 ${
                      isActive
                        ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-950'
                        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'
                    }`
                  }
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span className="min-w-0 truncate font-medium">{route.label}</span>
                </NavLink>
              )
            })}
          </nav>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="shrink-0 border-b border-slate-200 bg-white px-4 py-4 dark:border-slate-800 dark:bg-slate-950/95 sm:px-6 lg:px-8">
          <div className="mx-auto flex max-w-7xl flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="min-w-0">
              <h2 className="text-2xl font-semibold text-slate-900">{currentRoute.label}</h2>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">{currentRoute.description}</p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="inline-flex items-center rounded-2xl border border-slate-200 bg-white p-1 dark:border-slate-700 dark:bg-slate-900">
                {[
                  { value: 'system', label: '跟随系统', icon: Monitor },
                  { value: 'light', label: '浅色', icon: Sun },
                  { value: 'dark', label: '深色', icon: Moon },
                ].map((option) => {
                  const Icon = option.icon
                  const active = themeMode === option.value
                  return (
                    <button
                      key={option.value}
                      type="button"
                      className={`inline-flex min-h-9 items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium transition ${
                        active
                          ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-950'
                          : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'
                      }`}
                      onClick={() => setThemeMode(option.value as 'system' | 'light' | 'dark')}
                      title={option.label}
                    >
                      <Icon className="h-3.5 w-3.5" />
                      <span className="hidden xl:inline">{option.label}</span>
                    </button>
                  )
                })}
              </div>

              <button
                type="button"
                className="inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-2.5 text-sm font-medium text-emerald-900 transition hover:bg-emerald-100 dark:border-emerald-400/20 dark:bg-emerald-500/15 dark:text-emerald-100 dark:hover:bg-emerald-500/22"
                onClick={() => setAgentOpen(true)}
              >
                <Bot className="h-4 w-4" />
                Agent
              </button>

              <div className="text-right text-sm text-slate-500 dark:text-slate-300">
                <div className="font-medium text-slate-900 dark:text-slate-50">{profile?.realName || '管理员'}</div>
                <div>{profile?.productRole === 'ADMIN' ? '管理员账户' : '普通用户账户'}</div>
              </div>

              <button type="button" className="btn-secondary gap-2" onClick={handleLogout}>
                <LogOut className="h-4 w-4" />
                退出
              </button>
            </div>
          </div>
        </header>

        <main className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>

      {agentOpen ? (
        <div className="fixed inset-0 z-50 bg-slate-950/35 backdrop-blur-sm">
          <div className="flex h-full w-full items-stretch justify-center p-0 sm:p-4">
            <div className="flex h-full w-full max-w-7xl flex-col overflow-hidden bg-white shadow-2xl dark:bg-slate-950 sm:h-[calc(100vh-2rem)] sm:rounded-[32px] sm:border sm:border-slate-200 dark:sm:border-slate-800">
            <div className="flex shrink-0 items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
              <div className="flex items-center gap-3">
                <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-2 dark:border-emerald-400/20 dark:bg-emerald-500/15">
                  <Bot className="h-5 w-5 text-emerald-700 dark:text-emerald-200" />
                </div>
                <div>
                  <div className="text-base font-semibold text-slate-950 dark:text-slate-50">Agent 助手</div>
                  <div className="text-sm text-slate-500 dark:text-slate-300">查询、执行和补充参数都在这里完成。</div>
                </div>
              </div>
              <button type="button" className="btn-secondary gap-2" onClick={() => setAgentOpen(false)}>
                <X className="h-4 w-4" />
                关闭
              </button>
            </div>

              <div className="min-h-0 flex-1 p-3 sm:p-5">
                <BuiltInAgentPage embedded />
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
