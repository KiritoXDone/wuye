import { LogOut } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'

import { coreRoutes, getRouteMeta } from '@/router'
import { useAuthStore } from '@/stores/auth'

export default function AppShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const profile = useAuthStore((state) => state.profile)
  const logout = useAuthStore((state) => state.logout)
  const currentRoute = getRouteMeta(location.pathname)

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen bg-transparent">
      <aside className="hidden w-80 shrink-0 px-4 py-4 lg:flex lg:flex-col">
        <div className="glass-panel flex h-full flex-col gap-8 overflow-y-auto p-5 text-slate-900">
          <div className="space-y-3 rounded-3xl border border-white/70 bg-gradient-to-br from-cyan-50/90 via-white/80 to-blue-50/90 p-5">
            <span className="inline-flex rounded-full border border-white/80 bg-white/75 px-3 py-1 text-xs font-medium text-primary-800">
              运营中心
            </span>
            <div>
              <h1 className="text-2xl font-semibold text-slate-950">物业运营管理平台</h1>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                年度物业费、月度水费、账单复核与经营看板统一在同一控制台内完成。
              </p>
            </div>
          </div>

          <nav className="space-y-3" aria-label="主导航">
            {coreRoutes.map((route) => {
              const Icon = route.icon
              return (
                <NavLink
                  key={route.path}
                  to={route.path}
                  className={({ isActive }) =>
                    `group flex min-h-14 cursor-pointer items-start gap-3 rounded-2xl border px-4 py-3 transition duration-200 ${
                      isActive
                        ? 'border-primary-200 bg-primary-50/85 text-primary-900 shadow-sm backdrop-blur'
                        : 'border-white/60 bg-white/35 text-slate-600 hover:border-white/80 hover:bg-white/55 hover:text-slate-900'
                    }`
                  }
                >
                  <span className="mt-0.5 rounded-xl bg-white/75 p-2">
                    <Icon className="h-5 w-5" />
                  </span>
                  <span className="min-w-0">
                    <span className="block text-sm font-semibold">{route.label}</span>
                    <span className="mt-1 block text-xs leading-5 text-inherit/80">{route.description}</span>
                  </span>
                </NavLink>
              )
            })}
          </nav>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="px-4 pb-2 pt-4 sm:px-6 lg:px-8">
          <div className="mx-auto flex max-w-7xl flex-col gap-4 rounded-[28px] border border-white/70 bg-white/90 px-5 py-5 shadow-panel backdrop-blur sm:px-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-primary-700">
                <span className="tag">按房间统计</span>
                <span className="tag">年度物业费 + 月度水费</span>
              </div>
              <h2 className="mt-3 text-2xl font-semibold text-slate-900">{currentRoute.label}</h2>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">{currentRoute.description}</p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
                <div className="font-semibold text-slate-900">{profile?.realName || '管理员'}</div>
                <div className="mt-1 text-xs text-slate-500">
                  {profile?.groupIds?.length ? `已授权 ${profile.groupIds.length} 个用户组` : '全局管理范围'}
                </div>
              </div>
              <button type="button" className="btn-secondary gap-2" onClick={handleLogout}>
                <LogOut className="h-4 w-4" />
                安全退出
              </button>
            </div>
          </div>
        </header>

        <main className="px-4 pb-8 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
