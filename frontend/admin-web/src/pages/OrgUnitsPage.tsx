import { useEffect, useMemo, useState } from 'react'
import { Pencil, Plus, RefreshCcw, Trash2, X } from 'lucide-react'

import { createCommunity, deleteCommunity, getCommunities, updateCommunity } from '@/api/communities'
import { createRoomType, deleteRoomType, getRoomTypes } from '@/api/room-types'
import {
  batchCreateAdminRooms,
  batchDeleteAdminRooms,
  batchUpdateAdminRooms,
  createAdminRoom,
  deleteAdminRoom,
  getAdminRooms,
  updateAdminRoom,
} from '@/api/rooms'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import type { AdminCommunity, CommunityUpsertPayload } from '@/types/community'
import type {
  AdminRoom,
  AdminRoomBatchCreatePayload,
  AdminRoomBatchDeletePayload,
  AdminRoomBatchUpdatePayload,
  AdminRoomCreatePayload,
  AdminRoomListQuery,
  BatchOperationResult,
} from '@/types/room'
import type { RoomType, RoomTypeUpsertPayload } from '@/types/room-type'

const defaultCommunityForm: CommunityUpsertPayload = { communityCode: 'AUTO', name: '' }
const defaultRoomTypeForm = (communityId = 0): RoomTypeUpsertPayload => ({ communityId, typeCode: '', typeName: '', areaM2: 0 })
const defaultRoomForm = (communityId = 0): AdminRoomCreatePayload => ({ communityId, buildingNo: '', unitNo: '', roomNo: '', roomTypeId: null, areaM2: 0 })
const defaultRoomFilters = (communityId = 0): AdminRoomListQuery => ({ communityId, buildingNo: '', unitNo: '', roomNoKeyword: '', roomSuffix: '', roomTypeId: undefined })
const defaultBatchCreateForm = (communityId = 0): AdminRoomBatchCreatePayload => ({ communityId, buildingNo: '', unitNo: '', roomNos: [], roomTypeId: null, areaM2: 0 })
const defaultBatchUpdateForm = (communityId = 0): AdminRoomBatchUpdatePayload => ({ communityId, selectionRoomIds: [], applyToFiltered: false, buildingNo: '', unitNo: '', roomNoKeyword: '', roomSuffix: '', roomTypeId: undefined, targetRoomTypeId: undefined, targetAreaM2: undefined })

function parseRoomNos(text: string) {
  return text.split(/[\n,\s]+/).map((item) => item.trim()).filter(Boolean)
}

function formatBatchResult(result: BatchOperationResult) {
  const reasonText = result.skippedReasons.length ? `；跳过：${result.skippedReasons.join('；')}` : ''
  return `请求 ${result.requestedCount} 条，成功 ${result.successCount} 条，跳过 ${result.skippedCount} 条${reasonText}`
}

function Modal({ title, description, onClose, children }: { title: string; description?: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-sm">
      <div className="w-full max-w-3xl rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <div className="text-lg font-semibold text-slate-950">{title}</div>
            {description ? <div className="mt-1 text-sm text-slate-500">{description}</div> : null}
          </div>
          <button type="button" className="btn-secondary min-h-10 px-3 py-2" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  )
}

export default function OrgUnitsPage() {
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([])
  const [rooms, setRooms] = useState<AdminRoom[]>([])
  const [selectedCommunityId, setSelectedCommunityId] = useState<number | undefined>()
  const [selectedRoomIds, setSelectedRoomIds] = useState<number[]>([])
  const [communityForm, setCommunityForm] = useState(defaultCommunityForm)
  const [editingCommunityId, setEditingCommunityId] = useState<number | null>(null)
  const [roomTypeForm, setRoomTypeForm] = useState(defaultRoomTypeForm())
  const [roomForm, setRoomForm] = useState(defaultRoomForm())
  const [roomFilters, setRoomFilters] = useState<AdminRoomListQuery>(defaultRoomFilters())
  const [batchCreateForm, setBatchCreateForm] = useState(defaultBatchCreateForm())
  const [batchCreateText, setBatchCreateText] = useState('')
  const [batchUpdateForm, setBatchUpdateForm] = useState(defaultBatchUpdateForm())
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [showCommunityModal, setShowCommunityModal] = useState(false)
  const [showRoomTypeModal, setShowRoomTypeModal] = useState(false)
  const [showRoomModal, setShowRoomModal] = useState(false)
  const [showBatchCreateModal, setShowBatchCreateModal] = useState(false)
  const [showBatchUpdateModal, setShowBatchUpdateModal] = useState(false)
  const [showBatchDeleteModal, setShowBatchDeleteModal] = useState(false)

  const selectedCommunity = useMemo(() => communities.find((item) => item.id === selectedCommunityId), [communities, selectedCommunityId])
  const communityCount = communities.length
  const roomCount = useMemo(() => communities.reduce((sum, item) => sum + Number(item.roomCount || 0), 0), [communities])
  const roomTypeCount = roomTypes.length

  async function loadCommunities(preferredCommunityId?: number) {
    setLoading(true)
    setError('')
    try {
      const list = await getCommunities()
      setCommunities(list)
      const nextCommunityId = preferredCommunityId ?? selectedCommunityId ?? list[0]?.id
      if (nextCommunityId) {
        await loadCommunityDetail(nextCommunityId, undefined, list)
      } else {
        setSelectedCommunityId(undefined)
        setRoomTypes([])
        setRooms([])
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '房产档案加载失败')
    } finally {
      setLoading(false)
    }
  }

  async function loadCommunityDetail(communityId: number, filters?: AdminRoomListQuery, incomingCommunities?: AdminCommunity[]) {
    setDetailLoading(true)
    setError('')
    try {
      const nextFilters = filters ?? { ...roomFilters, communityId }
      const [roomTypeList, roomList] = await Promise.all([
        getRoomTypes(communityId),
        getAdminRooms(nextFilters),
      ])
      setSelectedCommunityId(communityId)
      setRoomTypes(roomTypeList)
      setRooms(roomList)
      setSelectedRoomIds([])
      setRoomFilters(nextFilters)
      setRoomTypeForm(defaultRoomTypeForm(communityId))
      setRoomForm(defaultRoomForm(communityId))
      setBatchCreateForm(defaultBatchCreateForm(communityId))
      setBatchUpdateForm(defaultBatchUpdateForm(communityId))
      if (incomingCommunities) setCommunities(incomingCommunities)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载小区详情失败')
    } finally {
      setDetailLoading(false)
    }
  }

  useEffect(() => {
    void loadCommunities()
  }, [])

  function toggleRoomSelection(roomId: number, checked: boolean) {
    setSelectedRoomIds((current) => checked ? Array.from(new Set([...current, roomId])) : current.filter((id) => id !== roomId))
  }

  function toggleSelectAll(checked: boolean) {
    setSelectedRoomIds(checked ? rooms.map((item) => item.id) : [])
  }

  async function handleSubmitCommunity() {
    if (!communityForm.name.trim()) {
      setError('请填写小区名称。')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      const payload = { communityCode: communityForm.communityCode.trim() || 'AUTO', name: communityForm.name.trim() }
      const community = editingCommunityId ? await updateCommunity(editingCommunityId, payload) : await createCommunity(payload)
      setShowCommunityModal(false)
      setEditingCommunityId(null)
      setCommunityForm(defaultCommunityForm)
      setMessage(editingCommunityId ? '小区更新成功。' : '小区创建成功。')
      await loadCommunities(community.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : '小区保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteCommunity(item: AdminCommunity) {
    if (!window.confirm(`确认删除小区“${item.name}”吗？`)) return
    setError('')
    setMessage('')
    try {
      await deleteCommunity(item.id)
      setMessage('小区已删除。')
      await loadCommunities()
    } catch (err) {
      setError(err instanceof Error ? err.message : '小区删除失败')
    }
  }

  async function handleCreateRoomType() {
    if (!selectedCommunityId || !roomTypeForm.typeName.trim() || Number(roomTypeForm.areaM2) <= 0) {
      setError('请填写完整户型信息。')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      await createRoomType({
        communityId: selectedCommunityId,
        typeCode: roomTypeForm.typeCode.trim() || roomTypeForm.typeName.trim(),
        typeName: roomTypeForm.typeName.trim(),
        areaM2: Number(roomTypeForm.areaM2),
      })
      setShowRoomTypeModal(false)
      setRoomTypeForm(defaultRoomTypeForm(selectedCommunityId))
      setMessage('户型创建成功。')
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '户型创建失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteRoomType(item: RoomType) {
    if (!selectedCommunityId) return
    if (!window.confirm(`确认删除户型 ${item.typeName} 吗？`)) return
    setError('')
    setMessage('')
    try {
      await deleteRoomType(item.id)
      setMessage('户型已删除。')
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '户型删除失败')
    }
  }

  async function handleQueryRooms() {
    if (!selectedCommunityId) return
    await loadCommunityDetail(selectedCommunityId, { ...roomFilters, communityId: selectedCommunityId })
  }

  async function handleCreateRoom() {
    if (!selectedCommunityId || !roomForm.buildingNo.trim() || !roomForm.unitNo.trim() || !roomForm.roomNo.trim() || Number(roomForm.areaM2) <= 0) {
      setError('请填写完整房间信息。')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      await createAdminRoom({ ...roomForm, communityId: selectedCommunityId, buildingNo: roomForm.buildingNo.trim(), unitNo: roomForm.unitNo.trim(), roomNo: roomForm.roomNo.trim(), areaM2: Number(roomForm.areaM2) })
      setShowRoomModal(false)
      setRoomForm(defaultRoomForm(selectedCommunityId))
      setMessage('房间创建成功。')
      await loadCommunities(selectedCommunityId)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '房间创建失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteRoom(item: AdminRoom) {
    const roomLabel = `${item.buildingNo}-${item.unitNo}-${item.roomNo}`
    if (!window.confirm(`确认删除房间 ${roomLabel} 吗？删除后不可恢复。`)) return
    setError('')
    setMessage('')
    try {
      await deleteAdminRoom(item.id)
      setMessage(`房间 ${roomLabel} 已删除。`)
      if (selectedCommunityId) {
        await loadCommunityDetail(selectedCommunityId)
        await loadCommunities(selectedCommunityId)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '房间删除失败')
    }
  }

  async function handleRoomTypeQuickBind(room: AdminRoom, roomTypeIdRaw: string) {
    const roomTypeId = roomTypeIdRaw ? Number(roomTypeIdRaw) : null
    const roomType = roomTypes.find((item) => item.id === roomTypeId)
    try {
      const updated = await updateAdminRoom(room.id, {
        roomTypeId,
        areaM2: roomType ? Number(roomType.areaM2) : Number(room.areaM2),
      })
      setRooms((current) => current.map((item) => (item.id === room.id ? updated : item)))
      setMessage(`房间 ${room.buildingNo}-${room.unitNo}-${room.roomNo} 已更新。`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '房间更新失败')
    }
  }

  async function handleBatchCreateRooms() {
    if (!selectedCommunityId || !batchCreateForm.buildingNo.trim() || !batchCreateForm.unitNo.trim() || Number(batchCreateForm.areaM2) <= 0) {
      setError('请填写完整批量新增信息。')
      return
    }
    const roomNos = parseRoomNos(batchCreateText)
    if (!roomNos.length) {
      setError('请至少填写一个房号。')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      const result = await batchCreateAdminRooms({ ...batchCreateForm, communityId: selectedCommunityId, buildingNo: batchCreateForm.buildingNo.trim(), unitNo: batchCreateForm.unitNo.trim(), roomNos, areaM2: Number(batchCreateForm.areaM2) })
      setShowBatchCreateModal(false)
      setBatchCreateText('')
      setMessage(`批量新增完成：${formatBatchResult(result)}`)
      await loadCommunities(selectedCommunityId)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量新增失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleBatchUpdateRooms(applyToFiltered: boolean) {
    if (!selectedCommunityId) return
    if (!batchUpdateForm.targetRoomTypeId && !batchUpdateForm.targetAreaM2) {
      setError('请先填写批量改户型或面积。')
      return
    }
    const affectedCount = applyToFiltered ? rooms.length : selectedRoomIds.length
    if (!affectedCount) {
      setError(applyToFiltered ? '当前筛选结果为空。' : '请先勾选房间。')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      const result = await batchUpdateAdminRooms({ ...roomFilters, ...batchUpdateForm, communityId: selectedCommunityId, applyToFiltered, selectionRoomIds: applyToFiltered ? [] : selectedRoomIds, targetAreaM2: batchUpdateForm.targetAreaM2 ? Number(batchUpdateForm.targetAreaM2) : undefined })
      setShowBatchUpdateModal(false)
      setMessage(`批量更新完成：${formatBatchResult(result)}`)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量更新失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleBatchDeleteRooms() {
    if (!selectedCommunityId) return
    if (!selectedRoomIds.length) {
      setError('请先勾选房间。')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      const payload: AdminRoomBatchDeletePayload = { ...roomFilters, communityId: selectedCommunityId, applyToFiltered: false, selectionRoomIds: selectedRoomIds }
      const result = await batchDeleteAdminRooms(payload)
      setShowBatchDeleteModal(false)
      setMessage(`批量删除完成：${formatBatchResult(result)}`)
      await loadCommunities(selectedCommunityId)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量删除失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div className="space-y-6 pb-2">
        <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div className="space-y-3">
              <div>
                <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">房间管理</div>
                <h1 className="mt-2 text-2xl font-semibold text-slate-950">小区直管房产档案</h1>
                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">以列表为主，新增和批量操作收口为弹出层，减少页面常驻表单。</p>
              </div>
              <div className="grid gap-3 sm:grid-cols-3">
                {[
                  ['小区数', String(communities.length)],
                  ['房间总数', String(communityCount ? roomCount : 0)],
                  ['当前户型数', String(roomTypeCount)],
                ].map(([label, value]) => (
                  <div key={label} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                    <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
                    <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
                  </div>
                ))}
              </div>
            </div>
            <button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={() => void loadCommunities(selectedCommunityId)} disabled={loading || detailLoading}><RefreshCcw className="h-4 w-4" />{loading || detailLoading ? '刷新中...' : '刷新'}</button>
          </div>
        </section>

        {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
        {message ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

        <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
          <PageSection title="小区管理" description="小区是房间管理一级主体。" action={<button type="button" className="btn-primary gap-2" onClick={() => { setEditingCommunityId(null); setCommunityForm(defaultCommunityForm); setShowCommunityModal(true) }}><Plus className="h-4 w-4" />新增小区</button>}>
            <AsyncState loading={loading} error={error} empty={!communities.length} emptyDescription="暂无小区数据。">
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead><tr className="border-b border-slate-200 text-slate-500"><th className="px-4 py-3 font-medium">小区</th><th className="px-4 py-3 font-medium">户型数</th><th className="px-4 py-3 font-medium">房间数</th><th className="px-4 py-3 font-medium text-right">操作</th></tr></thead>
                  <tbody>{communities.map((item) => <tr key={item.id} className={`border-b border-slate-100 last:border-0 ${selectedCommunityId === item.id ? 'bg-slate-50' : 'hover:bg-white/50'}`}><td className="px-4 py-4 font-medium text-slate-900">{item.name}</td><td className="px-4 py-4 text-slate-600">{item.roomTypeCount}</td><td className="px-4 py-4 text-slate-600">{item.roomCount}</td><td className="px-4 py-4 text-right"><div className="flex flex-wrap justify-end gap-2"><button type="button" className="btn-secondary min-h-9 px-3 py-1.5" onClick={() => { setSelectedCommunityId(item.id); void loadCommunityDetail(item.id, defaultRoomFilters(item.id)) }}>进入管理</button><button type="button" className="btn-secondary min-h-9 px-3 py-1.5" onClick={() => { setEditingCommunityId(item.id); setCommunityForm({ communityCode: item.communityCode || 'AUTO', name: item.name }); setShowCommunityModal(true) }}><Pencil className="h-4 w-4" /></button><button type="button" className="inline-flex min-h-9 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-1.5 text-sm font-medium text-rose-700 transition hover:bg-rose-100" onClick={() => void handleDeleteCommunity(item)}><Trash2 className="h-4 w-4" />删除</button></div></td></tr>)}</tbody>
                </table>
              </div>
            </AsyncState>
          </PageSection>

          <PageSection title="户型管理" description={selectedCommunity ? `${selectedCommunity.name} 的户型定义。` : '先选择一个小区。'} action={selectedCommunityId ? <button type="button" className="btn-primary gap-2" onClick={() => { setRoomTypeForm(defaultRoomTypeForm(selectedCommunityId)); setShowRoomTypeModal(true) }}><Plus className="h-4 w-4" />新增户型</button> : undefined}>
            <AsyncState loading={detailLoading} empty={!roomTypes.length} emptyDescription={selectedCommunityId ? '当前小区暂无户型。' : '请选择小区查看户型。'}>
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead><tr className="border-b border-slate-200 text-slate-500"><th className="px-4 py-3 font-medium">户型名称</th><th className="px-4 py-3 font-medium">面积</th><th className="px-4 py-3 font-medium text-right">操作</th></tr></thead>
                  <tbody>{roomTypes.map((item) => <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50"><td className="px-4 py-4 font-medium text-slate-900">{item.typeName}</td><td className="px-4 py-4 text-slate-600">{item.areaM2}</td><td className="px-4 py-4 text-right"><button type="button" className="inline-flex min-h-9 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-1.5 text-sm font-medium text-rose-700 transition hover:bg-rose-100" onClick={() => void handleDeleteRoomType(item)}><Trash2 className="h-4 w-4" />删除</button></td></tr>)}</tbody>
                </table>
              </div>
            </AsyncState>
          </PageSection>
        </div>

        <PageSection title="房间列表" description={selectedCommunity ? `${selectedCommunity.name} 下直接管理房间档案。` : '先选择一个小区。'} action={<div className="flex flex-wrap gap-2"><button type="button" className="btn-primary gap-2" onClick={() => selectedCommunityId && (setRoomForm(defaultRoomForm(selectedCommunityId)), setShowRoomModal(true))} disabled={!selectedCommunityId}><Plus className="h-4 w-4" />新增房间</button><button type="button" className="btn-secondary" onClick={() => setShowBatchUpdateModal(true)} disabled={!selectedCommunityId}>批量改户型/面积</button><button type="button" className="inline-flex min-h-11 items-center justify-center rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-sm font-medium text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => setShowBatchDeleteModal(true)} disabled={!selectedCommunityId}>批量删除</button></div>}>
          <div className="grid gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 md:grid-cols-2 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1.4fr)_220px_auto_auto] xl:items-end">
            <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">楼栋</span><input className="input" value={roomFilters.buildingNo || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, buildingNo: event.target.value }))} /></label>
            <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">单元</span><input className="input" value={roomFilters.unitNo || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, unitNo: event.target.value }))} /></label>
            <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">房号关键字</span><input className="input" value={roomFilters.roomNoKeyword || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, roomNoKeyword: event.target.value }))} /></label>
            <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">户型</span><select className="input" value={roomFilters.roomTypeId || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, roomTypeId: event.target.value ? Number(event.target.value) : undefined }))}><option value="">全部</option>{roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}</select></label>
            <button type="button" className="btn-secondary" onClick={() => { const reset = defaultRoomFilters(selectedCommunityId || 0); setRoomFilters(reset); if (selectedCommunityId) void loadCommunityDetail(selectedCommunityId, reset) }} disabled={!selectedCommunityId || detailLoading}>重置</button>
            <button type="button" className="btn-primary" onClick={() => void handleQueryRooms()} disabled={!selectedCommunityId || detailLoading}>查询</button>
          </div>
          <div className="mt-4 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">当前勾选 <span className="font-semibold text-slate-900">{selectedRoomIds.length}</span> 个房间，筛选结果 <span className="font-semibold text-slate-900">{rooms.length}</span> 条。</div>
          <AsyncState loading={detailLoading} empty={!rooms.length} emptyDescription={selectedCommunityId ? '当前条件下暂无房间。' : '请选择小区查看房间。'}>
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead><tr className="border-b border-slate-200 text-slate-500"><th className="px-4 py-3 font-medium"><input type="checkbox" checked={rooms.length > 0 && selectedRoomIds.length === rooms.length} onChange={(event) => toggleSelectAll(event.target.checked)} /></th><th className="px-4 py-3 font-medium">房间</th><th className="px-4 py-3 font-medium">当前户型</th><th className="px-4 py-3 font-medium">面积</th><th className="px-4 py-3 font-medium text-right">操作</th></tr></thead>
                <tbody>{rooms.map((item) => <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50"><td className="px-4 py-4"><input type="checkbox" checked={selectedRoomIds.includes(item.id)} onChange={(event) => toggleRoomSelection(item.id, event.target.checked)} /></td><td className="px-4 py-4 font-medium text-slate-900">{item.buildingNo}-{item.unitNo}-{item.roomNo}</td><td className="px-4 py-4 text-slate-600"><select className="input w-[140px] min-w-[140px]" value={item.roomTypeId || ''} onChange={(event) => void handleRoomTypeQuickBind(item, event.target.value)}><option value="">未绑定</option>{roomTypes.map((roomType) => <option key={roomType.id} value={roomType.id}>{roomType.typeName}</option>)}</select></td><td className="px-4 py-4 text-slate-600">{item.areaM2}</td><td className="px-4 py-4 text-right"><button type="button" className="inline-flex min-h-9 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-1.5 text-sm font-medium text-rose-700 transition hover:bg-rose-100" onClick={() => void handleDeleteRoom(item)}><Trash2 className="h-4 w-4" />删除</button></td></tr>)}</tbody>
              </table>
            </div>
          </AsyncState>
        </PageSection>
      </div>

      {showCommunityModal ? <Modal title={editingCommunityId ? '更新小区' : '新增小区'} onClose={() => setShowCommunityModal(false)}><div className="grid gap-4 lg:grid-cols-2"><label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">小区名称</span><input className="input" value={communityForm.name} onChange={(event) => setCommunityForm((current) => ({ ...current, name: event.target.value }))} /></label><label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">小区编码</span><input className="input" value={communityForm.communityCode} onChange={(event) => setCommunityForm((current) => ({ ...current, communityCode: event.target.value }))} /></label></div><div className="mt-5 flex justify-end gap-2"><button type="button" className="btn-secondary" onClick={() => setShowCommunityModal(false)}>取消</button><button type="button" className="btn-primary" onClick={() => void handleSubmitCommunity()} disabled={submitting}>{submitting ? '保存中...' : editingCommunityId ? '更新小区' : '新增小区'}</button></div></Modal> : null}
      {showRoomTypeModal ? <Modal title="新增户型" onClose={() => setShowRoomTypeModal(false)}><div className="grid gap-4 lg:grid-cols-2"><label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">户型名称</span><input className="input" value={roomTypeForm.typeName} onChange={(event) => setRoomTypeForm((current) => ({ ...current, typeName: event.target.value, typeCode: current.typeCode || event.target.value }))} /></label><label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">面积（㎡）</span><input className="input" type="number" min={0.01} step={0.01} value={roomTypeForm.areaM2 || ''} onChange={(event) => setRoomTypeForm((current) => ({ ...current, areaM2: Number(event.target.value) }))} /></label></div><div className="mt-5 flex justify-end"><button type="button" className="btn-primary" onClick={() => void handleCreateRoomType()} disabled={submitting}>{submitting ? '保存中...' : '新增户型'}</button></div></Modal> : null}
      {showRoomModal ? <Modal title="新增房间" onClose={() => setShowRoomModal(false)}><div className="grid gap-4 lg:grid-cols-2"><input className="input" placeholder="楼栋" value={roomForm.buildingNo} onChange={(event) => setRoomForm((current) => ({ ...current, buildingNo: event.target.value }))} /><input className="input" placeholder="单元" value={roomForm.unitNo} onChange={(event) => setRoomForm((current) => ({ ...current, unitNo: event.target.value }))} /><input className="input" placeholder="房号" value={roomForm.roomNo} onChange={(event) => setRoomForm((current) => ({ ...current, roomNo: event.target.value }))} /><select className="input" value={roomForm.roomTypeId || ''} onChange={(event) => { const nextRoomTypeId = event.target.value ? Number(event.target.value) : null; const roomType = roomTypes.find((item) => item.id === nextRoomTypeId); setRoomForm((current) => ({ ...current, roomTypeId: nextRoomTypeId, areaM2: roomType ? Number(roomType.areaM2) : current.areaM2 })); }}><option value="">未绑定户型</option>{roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}</select><input className="input lg:col-span-2" type="number" min={0.01} step={0.01} placeholder="面积（㎡）" value={roomForm.areaM2 || ''} onChange={(event) => setRoomForm((current) => ({ ...current, areaM2: Number(event.target.value) }))} /></div><div className="mt-5 flex justify-end"><button type="button" className="btn-primary" onClick={() => void handleCreateRoom()} disabled={submitting}>{submitting ? '提交中...' : '新增房间'}</button></div></Modal> : null}
      {showBatchCreateModal ? <Modal title="批量新增房间" onClose={() => setShowBatchCreateModal(false)}><div className="grid gap-4 xl:grid-cols-4"><input className="input" placeholder="楼栋" value={batchCreateForm.buildingNo} onChange={(event) => setBatchCreateForm((current) => ({ ...current, buildingNo: event.target.value }))} /><input className="input" placeholder="单元" value={batchCreateForm.unitNo} onChange={(event) => setBatchCreateForm((current) => ({ ...current, unitNo: event.target.value }))} /><select className="input" value={batchCreateForm.roomTypeId || ''} onChange={(event) => setBatchCreateForm((current) => ({ ...current, roomTypeId: event.target.value ? Number(event.target.value) : null }))}><option value="">未绑定户型</option>{roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}</select><input className="input" type="number" min={0.01} step={0.01} placeholder="面积（㎡）" value={batchCreateForm.areaM2 || ''} onChange={(event) => setBatchCreateForm((current) => ({ ...current, areaM2: Number(event.target.value) }))} /></div><textarea className="textarea mt-4" rows={5} placeholder="输入房号，支持逗号、空格或换行分隔" value={batchCreateText} onChange={(event) => setBatchCreateText(event.target.value)} /><div className="mt-5 flex justify-end"><button type="button" className="btn-primary" onClick={() => void handleBatchCreateRooms()} disabled={submitting}>{submitting ? '提交中...' : '批量新增'}</button></div></Modal> : null}
      {showBatchUpdateModal ? <Modal title="批量改户型 / 面积" onClose={() => setShowBatchUpdateModal(false)}><div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]"><select className="input" value={batchUpdateForm.targetRoomTypeId || ''} onChange={(event) => { const nextRoomTypeId = event.target.value ? Number(event.target.value) : undefined; const roomType = roomTypes.find((item) => item.id === nextRoomTypeId); setBatchUpdateForm((current) => ({ ...current, targetRoomTypeId: nextRoomTypeId, targetAreaM2: roomType ? Number(roomType.areaM2) : current.targetAreaM2 })); }}><option value="">不改户型</option>{roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}</select><input className="input" type="number" min={0.01} step={0.01} placeholder="新面积（㎡）" value={batchUpdateForm.targetAreaM2 || ''} onChange={(event) => setBatchUpdateForm((current) => ({ ...current, targetAreaM2: event.target.value ? Number(event.target.value) : undefined }))} /></div><div className="mt-5 flex justify-end gap-2"><button type="button" className="btn-primary" onClick={() => void handleBatchUpdateRooms(false)} disabled={submitting}>更新勾选房间</button><button type="button" className="btn-secondary" onClick={() => void handleBatchUpdateRooms(true)} disabled={submitting}>更新筛选结果</button></div></Modal> : null}
      {showBatchDeleteModal ? <Modal title="批量删除房间" onClose={() => setShowBatchDeleteModal(false)}><div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">当前勾选 {selectedRoomIds.length} 个房间。</div><div className="mt-5 flex justify-end"><button type="button" className="inline-flex min-h-10 items-center justify-center rounded-xl border border-rose-200 bg-rose-50 px-4 py-2 text-sm font-medium text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => void handleBatchDeleteRooms()} disabled={submitting}>删除勾选房间</button></div></Modal> : null}
    </>
  )
}
