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
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <aside className="hidden h-full w-72 shrink-0 border-r border-slate-200 bg-white lg:flex lg:flex-col">
        <div className="flex h-full min-h-0 flex-col gap-6 overflow-y-auto px-5 py-6 text-slate-900">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">运营后台</div>
            <h1 className="mt-2 text-xl font-semibold text-slate-950">物业运营管理平台</h1>
          </div>

          <nav className="space-y-1" aria-label="主导航">
            {coreRoutes.map((route) => {
              const Icon = route.icon
              return (
                <NavLink
                  key={route.path}
                  to={route.path}
                  className={({ isActive }) =>
                    `group flex min-h-11 cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition duration-150 ${
                      isActive
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
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
        <header className="shrink-0 border-b border-slate-200 bg-white px-4 py-4 sm:px-6 lg:px-8">
          <div className="mx-auto flex max-w-7xl flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="min-w-0">
              <h2 className="text-2xl font-semibold text-slate-900">{currentRoute.label}</h2>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">{currentRoute.description}</p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="text-right text-sm text-slate-500">
                <div className="font-medium text-slate-900">{profile?.realName || '管理员'}</div>
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
    </div>
  )
}
