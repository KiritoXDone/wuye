import { useEffect, useMemo, useState } from 'react'
import { Building2, Home, RefreshCcw } from 'lucide-react'

import { getOrgUnits } from '@/api/org-units'
import { createRoomType, getRoomTypes } from '@/api/room-types'
import { getAdminRooms, updateAdminRoom } from '@/api/rooms'
import type { RoomType } from '@/types/room-type'
import type { AdminRoom } from '@/types/room'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import type { OrgUnit } from '@/types/org-unit'

export default function OrgUnitsPage() {
  const [list, setList] = useState<OrgUnit[]>([])
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([])
  const [rooms, setRooms] = useState<AdminRoom[]>([])
  const [selectedCommunityId, setSelectedCommunityId] = useState<number | undefined>()
  const [roomTypeForm, setRoomTypeForm] = useState({ communityId: 0, typeCode: '', typeName: '', areaM2: 0 })
  const [loading, setLoading] = useState(false)
  const [roomTypeLoading, setRoomTypeLoading] = useState(false)
  const [roomTypeSubmitting, setRoomTypeSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

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

  async function loadRoomTypeData(communityId: number) {
    setRoomTypeLoading(true)
    setError('')
    try {
      const [roomTypeList, roomList] = await Promise.all([getRoomTypes(communityId), getAdminRooms(communityId)])
      setRoomTypes(roomTypeList)
      setRooms(roomList)
      setSelectedCommunityId(communityId)
      setRoomTypeForm((current) => ({ ...current, communityId }))
    } catch (err) {
      setError(err instanceof Error ? err.message : '户型数据加载失败')
    } finally {
      setRoomTypeLoading(false)
    }
  }

  async function handleCreateRoomType() {
    if (!roomTypeForm.communityId || !roomTypeForm.typeCode.trim() || !roomTypeForm.typeName.trim() || roomTypeForm.areaM2 <= 0) {
      setError('请填写完整户型信息。')
      return
    }
    setRoomTypeSubmitting(true)
    setError('')
    setMessage('')
    try {
      await createRoomType(roomTypeForm)
      setMessage('户型创建成功。')
      await loadRoomTypeData(roomTypeForm.communityId)
      setRoomTypeForm((current) => ({ ...current, typeCode: '', typeName: '', areaM2: 0 }))
    } catch (err) {
      setError(err instanceof Error ? err.message : '户型创建失败')
    } finally {
      setRoomTypeSubmitting(false)
    }
  }

  const communityCount = useMemo(() => new Set(list.map((item) => item.communityId).filter(Boolean)).size, [list])
  const rootCount = useMemo(() => list.filter((item) => !item.parentId).length, [list])
  const communityBoundCount = useMemo(() => list.filter((item) => !!item.communityId).length, [list])

  return (
    <div className="space-y-6 pb-2">
      <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">房间管理</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">小区、户型与房间</h1>
          </div>
          <button type="button" className="btn-secondary gap-2" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>

        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          {[
            ['组织单元数', String(list.length)],
            ['小区数', String(communityCount)],
            ['根节点数', String(rootCount)],
            ['已绑小区', String(communityBoundCount)],
          ].map(([label, value]) => (
            <div key={label} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
              <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
            </div>
          ))}
        </div>
      </section>

      {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
      <PageSection title="小区选择" description="先选择一个小区，再维护户型与房间。">
        <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="暂无可用小区数据。">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-4 py-3 font-medium">记录 ID</th>
                  <th className="px-4 py-3 font-medium">组织编码</th>
                  <th className="px-4 py-3 font-medium">组织名称</th>
                  <th className="px-4 py-3 font-medium">上级节点</th>
                  <th className="px-4 py-3 font-medium">小区</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="px-4 py-4 font-medium text-slate-900">{item.id}</td>
                    <td className="px-4 py-4 text-slate-600">{item.orgCode}</td>
                    <td className="px-4 py-4 text-slate-900">{item.name}</td>
                    <td className="px-4 py-4 text-slate-600">{item.parentName || '--'}</td>
                    <td className="px-4 py-4 text-slate-600">
                      {item.communityId ? (
                        <button type="button" className="btn-secondary min-h-9 px-3 py-1.5" onClick={() => void loadRoomTypeData(item.communityId!)}>
                          小区 {item.communityId}
                        </button>
                      ) : '--'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </AsyncState>
      </PageSection>

      <PageSection title="户型管理" description={selectedCommunityId ? `小区 ${selectedCommunityId}` : '先选择一个小区。'}>
        <div className="grid gap-4">
          <div className="grid gap-4 md:grid-cols-3">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">户型编码</span>
              <input className="input" value={roomTypeForm.typeCode} onChange={(event) => setRoomTypeForm((current) => ({ ...current, typeCode: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">户型名称</span>
              <input className="input" value={roomTypeForm.typeName} onChange={(event) => setRoomTypeForm((current) => ({ ...current, typeName: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">面积（㎡）</span>
              <input className="input" type="number" min={0.01} step={0.01} value={roomTypeForm.areaM2 || ''} onChange={(event) => setRoomTypeForm((current) => ({ ...current, areaM2: Number(event.target.value) }))} />
            </label>
          </div>
          <div>
            <button type="button" className="btn-primary" onClick={() => void handleCreateRoomType()} disabled={!selectedCommunityId || roomTypeSubmitting}>
              {roomTypeSubmitting ? '保存中...' : '新增户型'}
            </button>
          </div>
          <AsyncState loading={roomTypeLoading} empty={!roomTypes.length} emptyDescription={selectedCommunityId ? '当前小区暂无户型。' : '请选择小区查看户型。'}>
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">户型编码</th>
                    <th className="px-4 py-3 font-medium">户型名称</th>
                    <th className="px-4 py-3 font-medium">面积</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                  </tr>
                </thead>
                <tbody>
                  {roomTypes.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4 font-medium text-slate-900">{item.typeCode}</td>
                      <td className="px-4 py-4 text-slate-900">{item.typeName}</td>
                      <td className="px-4 py-4 text-slate-600">{item.areaM2}</td>
                      <td className="px-4 py-4 text-slate-600">{item.status === 1 ? '启用' : '停用'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </div>
      </PageSection>

      <PageSection title="房间户型绑定" description={selectedCommunityId ? '选择户型并同步面积。' : '先选择一个小区。'}>
        <AsyncState loading={roomTypeLoading} empty={!rooms.length} emptyDescription={selectedCommunityId ? '当前小区暂无房间。' : '请选择小区查看房间。'}>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-4 py-3 font-medium">房间</th>
                  <th className="px-4 py-3 font-medium">当前户型</th>
                  <th className="px-4 py-3 font-medium">面积</th>
                  <th className="px-4 py-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                {rooms.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="px-4 py-4 font-medium text-slate-900">{item.buildingNo}-{item.unitNo}-{item.roomNo}</td>
                    <td className="px-4 py-4 text-slate-600">{item.roomTypeName || '--'}</td>
                    <td className="px-4 py-4 text-slate-600">{item.areaM2}</td>
                    <td className="px-4 py-4">
                      <div className="flex flex-wrap items-center gap-2">
                        <select
                          className="input min-w-[160px]"
                          value={item.roomTypeId || ''}
                          onChange={async (event) => {
                            const roomTypeId = event.target.value ? Number(event.target.value) : null
                            const selectedType = roomTypes.find((roomType) => roomType.id === roomTypeId)
                            const updated = await updateAdminRoom(item.id, {
                              roomTypeId,
                              areaM2: selectedType ? Number(selectedType.areaM2) : Number(item.areaM2),
                            })
                            setRooms((current) => current.map((room) => room.id === item.id ? updated : room))
                          }}
                        >
                          <option value="">未绑定</option>
                          {roomTypes.map((roomType) => (
                            <option key={roomType.id} value={roomType.id}>{roomType.typeName}</option>
                          ))}
                        </select>
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
    </div>
  )
}
