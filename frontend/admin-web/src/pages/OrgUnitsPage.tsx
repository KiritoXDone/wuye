import { useEffect, useMemo, useState } from 'react'
import { Building2, Network, RefreshCcw } from 'lucide-react'

import { getOrgUnits } from '@/api/org-units'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import type { OrgUnit } from '@/types/org-unit'

export default function OrgUnitsPage() {
  const [list, setList] = useState<OrgUnit[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      setList(await getOrgUnits())
    } catch (err) {
      setError(err instanceof Error ? err.message : '组织架构加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  const tenantCount = useMemo(() => new Set(list.map((item) => item.tenantCode).filter(Boolean)).size, [list])
  const rootCount = useMemo(() => list.filter((item) => !item.parentId).length, [list])
  const communityBoundCount = useMemo(() => list.filter((item) => !!item.communityId).length, [list])

  return (
    <div className="space-y-6 pb-2">
      <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">组织架构</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">组织单元</h1>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>

        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          {[
            ['组织单元数', String(list.length)],
            ['租户数', String(tenantCount)],
            ['根组织数', String(rootCount)],
            ['绑定小区组织', String(communityBoundCount)],
          ].map(([label, value]) => (
            <div key={label} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
              <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
            </div>
          ))}
        </div>
      </section>

      <PageSection title="组织单元列表" description="当前后端返回 tenantCode、orgCode、parentName 与 communityId，用于治理核对。">
        <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无组织单元数据。">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-4 py-3 font-medium">组织 ID</th>
                  <th className="px-4 py-3 font-medium">租户编码</th>
                  <th className="px-4 py-3 font-medium">组织编码</th>
                  <th className="px-4 py-3 font-medium">组织名称</th>
                  <th className="px-4 py-3 font-medium">上级组织</th>
                  <th className="px-4 py-3 font-medium">小区 ID</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="px-4 py-4 font-medium text-slate-900">{item.id}</td>
                    <td className="px-4 py-4 text-slate-600">{item.tenantCode}</td>
                    <td className="px-4 py-4 text-slate-600">{item.orgCode}</td>
                    <td className="px-4 py-4 text-slate-900">{item.name}</td>
                    <td className="px-4 py-4 text-slate-600">{item.parentName || '--'}</td>
                    <td className="px-4 py-4 text-slate-600">{item.communityId || '--'}</td>
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
