import { useEffect, useMemo, useState } from 'react'
import { Plus, RefreshCcw, Trash2 } from 'lucide-react'

import { createCommunity, disableCommunity, getCommunities, updateCommunity } from '@/api/communities'
import { createRoomType, disableRoomType, getRoomTypes } from '@/api/room-types'
import {
  batchCreateAdminRooms,
  batchDeleteAdminRooms,
  batchUpdateAdminRooms,
  createAdminRoom,
  disableAdminRoom,
  enableAdminRoom,
  getAdminRooms,
  updateAdminRoom,
} from '@/api/rooms'
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
import type { RoomType } from '@/types/room-type'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'

const defaultCommunityForm: CommunityUpsertPayload = { communityCode: '', name: '', status: 1 }
const defaultRoomFilters = (communityId = 0): AdminRoomListQuery => ({ communityId, buildingNo: '', unitNo: '', roomNoKeyword: '', roomSuffix: '', roomTypeId: undefined, status: undefined })
const defaultRoomTypeForm = (communityId = 0) => ({ communityId, typeCode: '', typeName: '', areaM2: 0 })
const defaultCreateRoomForm = (communityId = 0): AdminRoomCreatePayload => ({ communityId, buildingNo: '', unitNo: '', roomNo: '', roomTypeId: null, areaM2: 0 })
const defaultBatchCreateForm = (communityId = 0): AdminRoomBatchCreatePayload => ({ communityId, buildingNo: '', unitNo: '', roomNos: [], roomTypeId: null, areaM2: 0 })
const defaultBatchUpdateForm = (communityId = 0): AdminRoomBatchUpdatePayload => ({ communityId, selectionRoomIds: [], applyToFiltered: false, buildingNo: '', unitNo: '', roomNoKeyword: '', roomSuffix: '', roomTypeId: undefined, status: undefined, targetRoomTypeId: undefined, targetAreaM2: undefined })

function parseRoomNos(text: string) {
  return text
    .split(/[,\n\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatBatchResult(result: BatchOperationResult) {
  const reasonText = result.skippedReasons.length ? `；跳过：${result.skippedReasons.join('；')}` : ''
  return `请求 ${result.requestedCount} 条，成功 ${result.successCount} 条，跳过 ${result.skippedCount} 条${reasonText}`
}

export default function OrgUnitsPage() {
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([])
  const [rooms, setRooms] = useState<AdminRoom[]>([])
  const [selectedCommunityId, setSelectedCommunityId] = useState<number | undefined>()
  const [communityForm, setCommunityForm] = useState(defaultCommunityForm)
  const [editingCommunityId, setEditingCommunityId] = useState<number | null>(null)
  const [roomTypeForm, setRoomTypeForm] = useState(defaultRoomTypeForm())
  const [roomFilters, setRoomFilters] = useState<AdminRoomListQuery>(defaultRoomFilters())
  const [createRoomForm, setCreateRoomForm] = useState<AdminRoomCreatePayload>(defaultCreateRoomForm())
  const [batchCreateText, setBatchCreateText] = useState('')
  const [batchCreateForm, setBatchCreateForm] = useState<AdminRoomBatchCreatePayload>(defaultBatchCreateForm())
  const [batchUpdateForm, setBatchUpdateForm] = useState<AdminRoomBatchUpdatePayload>(defaultBatchUpdateForm())
  const [selectedRoomIds, setSelectedRoomIds] = useState<number[]>([])
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [communitySubmitting, setCommunitySubmitting] = useState(false)
  const [roomTypeSubmitting, setRoomTypeSubmitting] = useState(false)
  const [roomSubmitting, setRoomSubmitting] = useState(false)
  const [batchSubmitting, setBatchSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const selectedCommunity = useMemo(
    () => communities.find((item) => item.id === selectedCommunityId),
    [communities, selectedCommunityId],
  )

  const communityCount = communities.length
  const roomCount = useMemo(() => communities.reduce((sum, item) => sum + Number(item.roomCount || 0), 0), [communities])
  const activeRoomCount = useMemo(() => rooms.filter((item) => item.status === 1).length, [rooms])
  const selectedCount = selectedRoomIds.length

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
      setError(err instanceof Error ? err.message : '房间管理数据加载失败')
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
      setRoomTypes(roomTypeList)
      setRooms(roomList)
      setSelectedCommunityId(communityId)
      setRoomFilters(nextFilters)
      setRoomTypeForm(defaultRoomTypeForm(communityId))
      setCreateRoomForm(defaultCreateRoomForm(communityId))
      setBatchCreateForm(defaultBatchCreateForm(communityId))
      setBatchUpdateForm(defaultBatchUpdateForm(communityId))
      setSelectedRoomIds([])
      if (incomingCommunities) {
        setCommunities(incomingCommunities)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '房间数据加载失败')
    } finally {
      setDetailLoading(false)
    }
  }

  useEffect(() => {
    void loadCommunities()
  }, [])

  function handleSelectCommunity(community: AdminCommunity) {
    setCommunityForm({ communityCode: community.communityCode, name: community.name, status: community.status })
    setEditingCommunityId(community.id)
    void loadCommunityDetail(community.id, defaultRoomFilters(community.id))
  }

  function resetCommunityForm() {
    setCommunityForm(defaultCommunityForm)
    setEditingCommunityId(null)
  }

  async function handleSubmitCommunity() {
    if (!communityForm.communityCode.trim() || !communityForm.name.trim()) {
      setError('请填写完整小区信息。')
      return
    }
    setCommunitySubmitting(true)
    setError('')
    setMessage('')
    try {
      const payload = {
        communityCode: communityForm.communityCode.trim(),
        name: communityForm.name.trim(),
        status: communityForm.status ?? 1,
      }
      const community = editingCommunityId
        ? await updateCommunity(editingCommunityId, payload)
        : await createCommunity(payload)
      setMessage(editingCommunityId ? '小区更新成功。' : '小区创建成功。')
      resetCommunityForm()
      await loadCommunities(community.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : '小区保存失败')
    } finally {
      setCommunitySubmitting(false)
    }
  }

  async function handleDisableCommunity(community: AdminCommunity) {
    if (!window.confirm(`确认停用小区“${community.name}”吗？停用后不会删除历史房间与账单数据。`)) {
      return
    }
    setError('')
    setMessage('')
    try {
      await disableCommunity(community.id)
      setMessage('小区已停用。')
      await loadCommunities(community.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : '小区停用失败')
    }
  }

  async function handleCreateRoomType() {
    if (!selectedCommunityId || !roomTypeForm.typeCode.trim() || !roomTypeForm.typeName.trim() || roomTypeForm.areaM2 <= 0) {
      setError('请填写完整户型信息。')
      return
    }
    setRoomTypeSubmitting(true)
    setError('')
    setMessage('')
    try {
      await createRoomType({
        communityId: selectedCommunityId,
        typeCode: roomTypeForm.typeCode.trim(),
        typeName: roomTypeForm.typeName.trim(),
        areaM2: Number(roomTypeForm.areaM2),
      })
      setMessage('户型创建成功。')
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '户型创建失败')
    } finally {
      setRoomTypeSubmitting(false)
    }
  }

  async function handleDisableRoomType(roomType: RoomType) {
    if (!selectedCommunityId || roomType.status === 0) {
      return
    }
    if (!window.confirm(`确认停用户型 ${roomType.typeName} 吗？已绑定房间将保留当前户型信息。`)) {
      return
    }
    setError('')
    setMessage('')
    try {
      await disableRoomType(roomType.id, {
        communityId: roomType.communityId,
        typeCode: roomType.typeCode,
        typeName: roomType.typeName,
        areaM2: Number(roomType.areaM2),
      })
      setMessage('户型已停用。')
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '户型停用失败')
    }
  }

  async function handleQueryRooms() {
    if (!selectedCommunityId) {
      return
    }
    await loadCommunityDetail(selectedCommunityId, { ...roomFilters, communityId: selectedCommunityId })
  }

  async function handleCreateRoom() {
    if (!selectedCommunityId || !createRoomForm.buildingNo.trim() || !createRoomForm.unitNo.trim() || !createRoomForm.roomNo.trim() || Number(createRoomForm.areaM2) <= 0) {
      setError('请填写完整房间信息。')
      return
    }
    setRoomSubmitting(true)
    setError('')
    setMessage('')
    try {
      await createAdminRoom({
        ...createRoomForm,
        communityId: selectedCommunityId,
        buildingNo: createRoomForm.buildingNo.trim(),
        unitNo: createRoomForm.unitNo.trim(),
        roomNo: createRoomForm.roomNo.trim(),
        areaM2: Number(createRoomForm.areaM2),
      })
      setMessage('房间创建成功。')
      await loadCommunities(selectedCommunityId)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '房间创建失败')
    } finally {
      setRoomSubmitting(false)
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

  async function handleToggleRoomStatus(room: AdminRoom) {
    const roomLabel = `${room.buildingNo}-${room.unitNo}-${room.roomNo}`
    const nextAction = room.status === 1 ? '停用' : '启用'
    if (!window.confirm(`确认${nextAction}房间 ${roomLabel} 吗？`)) {
      return
    }
    setError('')
    setMessage('')
    try {
      const updatedRoom = room.status === 1
        ? await disableAdminRoom(room.id)
        : await enableAdminRoom(room.id)

      const nextStatus = Number(updatedRoom.status)
      const filterConflicts = roomFilters.status !== undefined && roomFilters.status !== nextStatus
      const nextFilters = filterConflicts
        ? { ...roomFilters, communityId: selectedCommunityId || room.communityId, status: undefined }
        : { ...roomFilters, communityId: selectedCommunityId || room.communityId }

      if (selectedCommunityId) {
        await loadCommunityDetail(selectedCommunityId, nextFilters)
      }

      setMessage(
        filterConflicts
          ? `房间已${nextAction}，已切换为显示全部状态，便于查看最新结果。`
          : `房间已${nextAction}。`,
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : `房间${nextAction}失败`)
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
    if (!window.confirm(`确认在当前小区批量新增 ${roomNos.length} 个房间吗？`)) {
      return
    }
    setBatchSubmitting(true)
    setError('')
    setMessage('')
    try {
      const result = await batchCreateAdminRooms({
        ...batchCreateForm,
        communityId: selectedCommunityId,
        buildingNo: batchCreateForm.buildingNo.trim(),
        unitNo: batchCreateForm.unitNo.trim(),
        roomNos,
        areaM2: Number(batchCreateForm.areaM2),
      })
      setMessage(`批量新增完成：${formatBatchResult(result)}`)
      setBatchCreateText('')
      await loadCommunities(selectedCommunityId)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量新增失败')
    } finally {
      setBatchSubmitting(false)
    }
  }

  async function handleBatchUpdateRooms(applyToFiltered: boolean) {
    if (!selectedCommunityId) {
      return
    }
    if (!batchUpdateForm.targetRoomTypeId && !batchUpdateForm.targetAreaM2) {
      setError('请先填写批量改户型或面积。')
      return
    }
    const affectedCount = applyToFiltered ? rooms.length : selectedRoomIds.length
    if (!affectedCount) {
      setError(applyToFiltered ? '当前筛选结果为空。' : '请先勾选房间。')
      return
    }
    if (!window.confirm(`确认批量更新 ${affectedCount} 个房间吗？`)) {
      return
    }
    setBatchSubmitting(true)
    setError('')
    setMessage('')
    try {
      const result = await batchUpdateAdminRooms({
        ...roomFilters,
        ...batchUpdateForm,
        communityId: selectedCommunityId,
        applyToFiltered,
        selectionRoomIds: applyToFiltered ? [] : selectedRoomIds,
        targetAreaM2: batchUpdateForm.targetAreaM2 ? Number(batchUpdateForm.targetAreaM2) : undefined,
      })
      setMessage(`批量更新完成：${formatBatchResult(result)}`)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量更新失败')
    } finally {
      setBatchSubmitting(false)
    }
  }

  async function handleBatchDeleteRooms(applyToFiltered: boolean) {
    if (!selectedCommunityId) {
      return
    }
    const affectedCount = applyToFiltered ? rooms.length : selectedRoomIds.length
    if (!affectedCount) {
      setError(applyToFiltered ? '当前筛选结果为空。' : '请先勾选房间。')
      return
    }
    if (!window.confirm(`确认停用 ${affectedCount} 个房间吗？历史账单和绑定记录会保留。`)) {
      return
    }
    setBatchSubmitting(true)
    setError('')
    setMessage('')
    try {
      const payload: AdminRoomBatchDeletePayload = {
        ...roomFilters,
        communityId: selectedCommunityId,
        applyToFiltered,
        selectionRoomIds: applyToFiltered ? [] : selectedRoomIds,
      }
      const result = await batchDeleteAdminRooms(payload)
      setMessage(`批量停用完成：${formatBatchResult(result)}`)
      await loadCommunities(selectedCommunityId)
      await loadCommunityDetail(selectedCommunityId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量停用失败')
    } finally {
      setBatchSubmitting(false)
    }
  }

  function toggleRoomSelection(roomId: number, checked: boolean) {
    setSelectedRoomIds((current) => checked ? Array.from(new Set([...current, roomId])) : current.filter((id) => id !== roomId))
  }

  function toggleSelectAll(checked: boolean) {
    setSelectedRoomIds(checked ? rooms.map((item) => item.id) : [])
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div className="space-y-3">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">房间管理</div>
              <h1 className="mt-2 text-2xl font-semibold text-slate-950">小区直管房产档案</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">收口小区、户型、房间与批量操作，减少大块堆叠和横向挤压，保留现有管理能力。</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              {[
                ['小区数', String(communityCount)],
                ['房间总数', String(roomCount)],
                ['当前启用房间', String(activeRoomCount)],
              ].map(([label, value]) => (
                <div key={label} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
                  <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
                </div>
              ))}
            </div>
          </div>
          <button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={() => void loadCommunities(selectedCommunityId)} disabled={loading || detailLoading}>
            <RefreshCcw className="h-4 w-4" />
            {loading || detailLoading ? '刷新中...' : '刷新'}
          </button>
        </div>
      </section>

      {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</div> : null}
      {message ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}

      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <PageSection title="小区管理" description="小区是房间管理一级主体，支持新增、编辑、停用。">
          <div className="grid gap-4">
            <div className="grid gap-4 md:grid-cols-3">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">小区编码</span>
                <input className="input" value={communityForm.communityCode} onChange={(event) => setCommunityForm((current) => ({ ...current, communityCode: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">小区名称</span>
                <input className="input" value={communityForm.name} onChange={(event) => setCommunityForm((current) => ({ ...current, name: event.target.value }))} />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">状态</span>
                <select className="input" value={communityForm.status ?? 1} onChange={(event) => setCommunityForm((current) => ({ ...current, status: Number(event.target.value) }))}>
                  <option value={1}>启用</option>
                  <option value={0}>停用</option>
                </select>
              </label>
            </div>
            <div className="flex flex-wrap gap-2">
              <button type="button" className="btn-primary gap-2" onClick={() => void handleSubmitCommunity()} disabled={communitySubmitting}>
                <Plus className="h-4 w-4" />
                {communitySubmitting ? '保存中...' : editingCommunityId ? '更新小区' : '新增小区'}
              </button>
              {editingCommunityId ? <button type="button" className="btn-secondary" onClick={resetCommunityForm}>取消编辑</button> : null}
            </div>
            <AsyncState loading={loading} error={error} empty={!communities.length} emptyDescription="暂无小区数据。">
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-slate-500">
                      <th className="px-4 py-3 font-medium">小区</th>
                      <th className="px-4 py-3 font-medium">编码</th>
                      <th className="px-4 py-3 font-medium">户型数</th>
                      <th className="px-4 py-3 font-medium">房间数</th>
                      <th className="px-4 py-3 font-medium">状态</th>
                      <th className="px-4 py-3 font-medium">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {communities.map((item) => (
                      <tr key={item.id} className={`border-b border-slate-100 last:border-0 ${selectedCommunityId === item.id ? 'bg-slate-50' : 'hover:bg-white/50'}`}>
                        <td className="px-4 py-4 font-medium text-slate-900">{item.name}</td>
                        <td className="px-4 py-4 text-slate-600">{item.communityCode}</td>
                        <td className="px-4 py-4 text-slate-600">{item.roomTypeCount}</td>
                        <td className="px-4 py-4 text-slate-600">{item.roomCount}</td>
                        <td className="px-4 py-4 text-slate-600">{item.status === 1 ? '启用' : '停用'}</td>
                        <td className="px-4 py-4">
                          <div className="flex flex-wrap gap-2">
                            <button type="button" className="btn-secondary min-h-9 px-3 py-1.5" onClick={() => handleSelectCommunity(item)}>进入管理</button>
                            <button type="button" className="btn-secondary min-h-9 px-3 py-1.5" onClick={() => { setEditingCommunityId(item.id); setCommunityForm({ communityCode: item.communityCode, name: item.name, status: item.status }) }}>编辑</button>
                            <button type="button" className="btn-secondary min-h-9 px-3 py-1.5 text-rose-600" onClick={() => void handleDisableCommunity(item)}>
                              <Trash2 className="mr-1 inline h-4 w-4" />停用
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AsyncState>
          </div>
        </PageSection>

        <PageSection title="户型管理" description={selectedCommunity ? `${selectedCommunity.name} 的户型定义` : '先选择一个小区。'}>
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
            <AsyncState loading={detailLoading} empty={!roomTypes.length} emptyDescription={selectedCommunityId ? '当前小区暂无户型。' : '请选择小区查看户型。'}>
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-slate-500">
                      <th className="whitespace-nowrap px-4 py-3 font-medium">户型编码</th>
                      <th className="whitespace-nowrap px-4 py-3 font-medium">户型名称</th>
                      <th className="whitespace-nowrap px-4 py-3 font-medium">面积</th>
                      <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                      <th className="whitespace-nowrap px-4 py-3 font-medium">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roomTypes.map((item) => (
                      <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                        <td className="whitespace-nowrap px-4 py-4 font-medium text-slate-900">{item.typeCode}</td>
                        <td className="whitespace-nowrap px-4 py-4 text-slate-900">{item.typeName}</td>
                        <td className="whitespace-nowrap px-4 py-4 text-slate-600">{item.areaM2}</td>
                        <td className="whitespace-nowrap px-4 py-4 text-slate-600">{item.status === 1 ? '启用' : '停用'}</td>
                        <td className="px-4 py-4">
                          <button type="button" className="btn-secondary min-h-9 whitespace-nowrap px-3 py-1.5 text-rose-600 disabled:text-slate-400" onClick={() => void handleDisableRoomType(item)} disabled={item.status === 0}>
                            {item.status === 0 ? '已停用' : '停用'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AsyncState>
          </div>
        </PageSection>
      </div>

      <PageSection
        title="房间列表"
        description={selectedCommunity ? `${selectedCommunity.name} 下直接管理房间档案。` : '先选择一个小区。'}
        action={<button type="button" className="btn-secondary whitespace-nowrap" onClick={() => void handleQueryRooms()} disabled={!selectedCommunityId || detailLoading}>查询</button>}
      >
        <div className="grid gap-4">
          <div className="grid gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 lg:grid-cols-[repeat(5,minmax(0,1fr))_auto] lg:items-end">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">楼栋</span>
              <input className="input" value={roomFilters.buildingNo || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, buildingNo: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">单元</span>
              <input className="input" value={roomFilters.unitNo || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, unitNo: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">房号关键字</span>
              <input className="input" value={roomFilters.roomNoKeyword || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, roomNoKeyword: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">户型</span>
              <select className="input" value={roomFilters.roomTypeId || ''} onChange={(event) => setRoomFilters((current) => ({ ...current, roomTypeId: event.target.value ? Number(event.target.value) : undefined }))}>
                <option value="">全部</option>
                {roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}{item.status === 0 ? '（已停用）' : ''}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">状态</span>
              <select className="input" value={roomFilters.status ?? ''} onChange={(event) => setRoomFilters((current) => ({ ...current, status: event.target.value === '' ? undefined : Number(event.target.value) }))}>
                <option value="">全部</option>
                <option value={1}>启用</option>
                <option value={0}>停用</option>
              </select>
            </label>
            <button type="button" className="btn-secondary whitespace-nowrap lg:px-5" onClick={() => { const reset = defaultRoomFilters(selectedCommunityId || 0); setRoomFilters(reset); if (selectedCommunityId) void loadCommunityDetail(selectedCommunityId, reset) }} disabled={!selectedCommunityId || detailLoading}>重置</button>
          </div>

          <div className="grid gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 md:grid-cols-5">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">新增楼栋</span>
              <input className="input" value={createRoomForm.buildingNo} onChange={(event) => setCreateRoomForm((current) => ({ ...current, buildingNo: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">新增单元</span>
              <input className="input" value={createRoomForm.unitNo} onChange={(event) => setCreateRoomForm((current) => ({ ...current, unitNo: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">新增房号</span>
              <input className="input" value={createRoomForm.roomNo} onChange={(event) => setCreateRoomForm((current) => ({ ...current, roomNo: event.target.value }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">户型</span>
              <select className="input" value={createRoomForm.roomTypeId || ''} onChange={(event) => setCreateRoomForm((current) => ({ ...current, roomTypeId: event.target.value ? Number(event.target.value) : null }))}>
                <option value="">未绑定</option>
                {roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">面积（㎡）</span>
              <input className="input" type="number" min={0.01} step={0.01} value={createRoomForm.areaM2 || ''} onChange={(event) => setCreateRoomForm((current) => ({ ...current, areaM2: Number(event.target.value) }))} />
            </label>
          </div>
          <div>
            <button type="button" className="btn-primary" onClick={() => void handleCreateRoom()} disabled={!selectedCommunityId || roomSubmitting}>
              {roomSubmitting ? '保存中...' : '新增房间'}
            </button>
          </div>

          <AsyncState loading={detailLoading} empty={!rooms.length} emptyDescription={selectedCommunityId ? '当前条件下暂无房间。' : '请选择小区查看房间。'}>
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-4 py-3 font-medium">
                      <input type="checkbox" checked={rooms.length > 0 && selectedRoomIds.length === rooms.length} onChange={(event) => toggleSelectAll(event.target.checked)} />
                    </th>
                    <th className="px-4 py-3 font-medium">房间</th>
                    <th className="px-4 py-3 font-medium">当前户型</th>
                    <th className="px-4 py-3 font-medium">面积</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {rooms.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                      <td className="px-4 py-4">
                        <input type="checkbox" checked={selectedRoomIds.includes(item.id)} onChange={(event) => toggleRoomSelection(item.id, event.target.checked)} />
                      </td>
                      <td className="px-4 py-4 font-medium text-slate-900">{item.buildingNo}-{item.unitNo}-{item.roomNo}</td>
                      <td className="px-4 py-4 text-slate-600">{item.roomTypeName || '--'}</td>
                      <td className="px-4 py-4 text-slate-600">{item.areaM2}</td>
                      <td className="px-4 py-4 text-slate-600">{item.status === 1 ? '启用' : '停用'}</td>
                      <td className="px-4 py-4">
                        <div className="flex flex-wrap items-center gap-2">
                          <select
                            className="input min-w-[160px]"
                            value={item.roomTypeId || ''}
                            onChange={(event) => void handleRoomTypeQuickBind(item, event.target.value)}
                          >
                            <option value="">未绑定</option>
                            {roomTypes.map((roomType) => (
                              <option key={roomType.id} value={roomType.id}>{roomType.typeName}</option>
                            ))}
                          </select>
                          <button
                            type="button"
                            className={`btn-secondary min-h-9 whitespace-nowrap px-3 py-1.5 ${item.status === 1 ? 'text-rose-600' : 'text-emerald-700'}`}
                            onClick={() => void handleToggleRoomStatus(item)}
                          >
                            {item.status === 1 ? '停用' : '启用'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </AsyncState>
        </div>
      </PageSection>

      <PageSection title="批量操作" description="支持按筛选结果全集执行，或仅对当前勾选房间执行。">
        <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
          当前勾选 <span className="font-semibold text-slate-900">{selectedCount}</span> 个房间，当前筛选结果 <span className="font-semibold text-slate-900">{rooms.length}</span> 条。
        </div>
        <div className="grid gap-6 xl:grid-cols-3">
          <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div>
              <div className="text-sm font-semibold text-slate-900">批量新增房间</div>
              <p className="mt-1 text-sm text-slate-500">同一小区内批量创建房号，重复项会返回跳过明细。</p>
            </div>
            <input className="input" placeholder="楼栋" value={batchCreateForm.buildingNo} onChange={(event) => setBatchCreateForm((current) => ({ ...current, buildingNo: event.target.value }))} />
            <input className="input" placeholder="单元" value={batchCreateForm.unitNo} onChange={(event) => setBatchCreateForm((current) => ({ ...current, unitNo: event.target.value }))} />
            <select className="input" value={batchCreateForm.roomTypeId || ''} onChange={(event) => setBatchCreateForm((current) => ({ ...current, roomTypeId: event.target.value ? Number(event.target.value) : null }))}>
              <option value="">未绑定户型</option>
              {roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}
            </select>
            <input className="input" type="number" min={0.01} step={0.01} placeholder="面积（㎡）" value={batchCreateForm.areaM2 || ''} onChange={(event) => setBatchCreateForm((current) => ({ ...current, areaM2: Number(event.target.value) }))} />
            <textarea className="textarea" rows={6} placeholder="输入房号，支持逗号、空格或换行分隔" value={batchCreateText} onChange={(event) => setBatchCreateText(event.target.value)} />
            <button type="button" className="btn-primary w-full" onClick={() => void handleBatchCreateRooms()} disabled={!selectedCommunityId || batchSubmitting}>
              {batchSubmitting ? '提交中...' : '批量新增'}
            </button>
          </div>

          <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div>
              <div className="text-sm font-semibold text-slate-900">批量改户型 / 面积</div>
              <p className="mt-1 text-sm text-slate-500">当前已勾选 {selectedCount} 个房间，也可直接作用于当前筛选结果 {rooms.length} 条。</p>
            </div>
            <select className="input" value={batchUpdateForm.targetRoomTypeId || ''} onChange={(event) => setBatchUpdateForm((current) => ({ ...current, targetRoomTypeId: event.target.value ? Number(event.target.value) : undefined }))}>
              <option value="">不改户型</option>
              {roomTypes.map((item) => <option key={item.id} value={item.id}>{item.typeName}</option>)}
            </select>
            <input className="input" type="number" min={0.01} step={0.01} placeholder="新面积（㎡）" value={batchUpdateForm.targetAreaM2 || ''} onChange={(event) => setBatchUpdateForm((current) => ({ ...current, targetAreaM2: event.target.value ? Number(event.target.value) : undefined }))} />
            <div className="grid gap-2 sm:grid-cols-2">
              <button type="button" className="btn-primary" onClick={() => void handleBatchUpdateRooms(false)} disabled={!selectedCommunityId || batchSubmitting}>更新勾选房间</button>
              <button type="button" className="btn-secondary" onClick={() => void handleBatchUpdateRooms(true)} disabled={!selectedCommunityId || batchSubmitting}>更新筛选结果</button>
            </div>
          </div>

          <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div>
              <div className="text-sm font-semibold text-slate-900">批量停用房间</div>
              <p className="mt-1 text-sm text-slate-500">删除语义统一为停用，不破坏历史账单、绑定、抄表与支付链路。</p>
            </div>
            <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
              当前勾选 {selectedCount} 个房间；当前筛选结果 {rooms.length} 条。执行前会二次确认影响数量。
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              <button type="button" className="btn-secondary text-rose-600" onClick={() => void handleBatchDeleteRooms(false)} disabled={!selectedCommunityId || batchSubmitting}>停用勾选房间</button>
              <button type="button" className="btn-secondary text-rose-600" onClick={() => void handleBatchDeleteRooms(true)} disabled={!selectedCommunityId || batchSubmitting}>停用筛选结果</button>
            </div>
          </div>
        </div>
      </PageSection>
    </div>
  )
}
