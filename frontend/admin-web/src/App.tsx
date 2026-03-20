import React, { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'

import AppShell from '@/layouts/AppShell'
import BillsPage from '@/pages/BillsPage'
import BillingGeneratePage from '@/pages/BillingGeneratePage'
import AgentGroupsPage from '@/pages/AgentGroupsPage'
import AiRuntimeConfigPage from '@/pages/AiRuntimeConfigPage'
import AuditLogsPage from '@/pages/AuditLogsPage'
import CouponRulesPage from '@/pages/CouponRulesPage'
import CouponTemplatesPage from '@/pages/CouponTemplatesPage'
import DashboardPage from '@/pages/DashboardPage'
import DunningPage from '@/pages/DunningPage'
import FeeRulesPage from '@/pages/FeeRulesPage'
import ImportExportPage from '@/pages/ImportExportPage'
import InvoiceApplicationsPage from '@/pages/InvoiceApplicationsPage'
import LoginPage from '@/pages/LoginPage'
import OrgUnitsPage from '@/pages/OrgUnitsPage'
import WaterAlertsPage from '@/pages/WaterAlertsPage'
import WaterReadingsPage from '@/pages/WaterReadingsPage'
import { useAuthStore } from '@/stores/auth'

function ProtectedApp() {
  const location = useLocation()
  const hasToken = useAuthStore((state) => state.hasToken)
  const profileLoaded = useAuthStore((state) => state.profileLoaded)
  const fetchProfile = useAuthStore((state) => state.fetchProfile)
  const clearSession = useAuthStore((state) => state.clearSession)
  const [booting, setBooting] = useState(hasToken && !profileLoaded)

  useEffect(() => {
    let mounted = true

    async function bootstrap() {
      if (hasToken && !profileLoaded) {
        setBooting(true)
        try {
          await fetchProfile()
        } catch {
          clearSession()
        } finally {
          if (mounted) {
            setBooting(false)
          }
        }
      } else {
        setBooting(false)
      }
    }

    void bootstrap()

    return () => {
      mounted = false
    }
  }, [clearSession, fetchProfile, hasToken, profileLoaded])

  if (!hasToken) {
    const redirect = `${location.pathname}${location.search}`
    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />
  }

  if (booting && !profileLoaded) {
    return <div className="flex min-h-screen items-center justify-center text-sm text-slate-500">正在恢复登录状态...</div>
  }

  return <Outlet />
}

function GuestOnly({ children }: { children: React.ReactElement }) {
  const hasToken = useAuthStore((state) => state.hasToken)
  return hasToken ? <Navigate to="/dashboard" replace /> : children
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <GuestOnly>
              <LoginPage />
            </GuestOnly>
          }
        />
        <Route element={<ProtectedApp />}>
          <Route element={<AppShell />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/billing-generate" element={<BillingGeneratePage />} />
            <Route path="/water-readings" element={<WaterReadingsPage />} />
            <Route path="/water-alerts" element={<WaterAlertsPage />} />
            <Route path="/import-export" element={<ImportExportPage />} />
            <Route path="/fee-rules" element={<FeeRulesPage />} />
            <Route path="/org-units" element={<OrgUnitsPage />} />
            <Route path="/agent-groups" element={<AgentGroupsPage />} />
            <Route path="/audit-logs" element={<AuditLogsPage />} />
            <Route path="/ai-runtime-config" element={<AiRuntimeConfigPage />} />
            <Route path="/dunning" element={<DunningPage />} />
            <Route path="/coupon-templates" element={<CouponTemplatesPage />} />
            <Route path="/coupon-rules" element={<CouponRulesPage />} />
            <Route path="/invoice-applications" element={<InvoiceApplicationsPage />} />
            <Route path="/bills" element={<BillsPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
