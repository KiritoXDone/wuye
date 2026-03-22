import { useEffect, useMemo, useState } from 'react'
import { RefreshCcw, Trash2 } from 'lucide-react'

import { getCommunities } from '@/api/communities'
import { getAdminRooms } from '@/api/rooms'
import { createWaterReading, deleteWaterReading, getWaterReadings } from '@/api/water'
import AsyncState from '@/components/ui/AsyncState'
import PageSection from '@/components/ui/PageSection'
import StatusBadge from '@/components/ui/StatusBadge'
import type { AdminCommunity } from '@/types/community'
import type { AdminRoom } from '@/types/room'
import { formatDateTime, formatPeriod, formatQuantity } from '@/utils/format'
import type { WaterReading } from '@/types/water'

const now = new Date()
const initialForm = {
  roomId: 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  prevReading: 0,
  currReading: 0,
  readAt: new Date().toISOString().slice(0, 16),
  remark: '',
}

const initialRoomFilters = {
  buildingNo: '',
  unitNo: '',
  roomNoKeyword: '',
}

export default function WaterReadingsPage() {
  const [filters, setFilters] = useState({ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 })
  const [communities, setCommunities] = useState<AdminCommunity[]>([])
  const [rooms, setRooms] = useState<AdminRoom[]>([])
  const [selectedCommunityId, setSelectedCommunityId] = useState<number | undefined>()
  const [roomFilters, setRoomFilters] = useState(initialRoomFilters)
  const [form, setForm] = useState(initialForm)
  const [list, setList] = useState<WaterReading[]>([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [roomLoading, setRoomLoading] = useState(false)
  const [error, setError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [submitResult, setSubmitResult] = useState('')

  const usagePreview = useMemo(() => Math.max(Number(form.currReading) - Number(form.prevReading), 0), [form.currReading, form.prevReading])
  const roomFiltersReady = Boolean(selectedCommunityId && roomFilters.buildingNo.trim() && roomFilters.unitNo.trim() && roomFilters.roomNoKeyword.trim())
  const matchedRooms = useMemo(() => {
    if (!roomFiltersReady) {
      return []
    }
    return rooms
  }, [roomFiltersReady, rooms])
  const selectedRoom = useMemo(() => matchedRooms.find((item) => item.id === Number(form.roomId)), [matchedRooms, form.roomId])
  const uniqueMatchedRoom = matchedRooms.length === 1 ? matchedRooms[0] : undefined

  async function loadData(nextFilters = filters) {
    setLoading(true)
    setError('')
    try {
      const result = await getWaterReadings(nextFilters)
      setList(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '抄表记录加载失败')
    } finally {
      setLoading(false)
    }
  }

  async function loadCommunitiesAndRooms() {
    setRoomLoading(true)
    setError('')
    try {
      const communityList = await getCommunities()
      setCommunities(communityList)
      const firstCommunityId = communityList[0]?.id
      setSelectedCommunityId(firstCommunityId)
      setRooms([])
    } catch (err) {
      setError(err instanceof Error ? err.message : '房间数据加载失败')
    } finally {
      setRoomLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
    void loadCommunitiesAndRooms()
  }, [])

  useEffect(() => {
    if (!selectedCommunityId) {
      setRooms([])
      setRoomFilters(initialRoomFilters)
      setForm((current) => ({ ...current, roomId: 0 }))
      return
    }

    const buildingNo = roomFilters.buildingNo.trim()
    const unitNo = roomFilters.unitNo.trim()
    const roomNoKeyword = roomFilters.roomNoKeyword.trim()

    setForm((current) => ({ ...current, roomId: 0 }))

    if (!buildingNo || !unitNo || !roomNoKeyword) {
      setRooms([])
      return
    }

    setRoomLoading(true)
    setError('')
    void getAdminRooms({ communityId: selectedCommunityId, buildingNo, unitNo, roomNoKeyword })
      .then((roomList) => {
        setRooms(roomList)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : '房间数据加载失败')
      })
      .finally(() => {
        setRoomLoading(false)
      })
  }, [selectedCommunityId, roomFilters])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setSubmitError('')
    setSubmitResult('')

    if (!roomFiltersReady) {
      setSubmitError('请先补全楼号、单元号和户号。')
      setSubmitting(false)
      return
    }

    if (!matchedRooms.length) {
      setSubmitError('未找到匹配房间，请检查楼号、单元号和户号。')
      setSubmitting(false)
      return
    }

    if (matchedRooms.length > 1) {
      setSubmitError('匹配到多个房间，请继续收窄楼号、单元号或户号。')
      setSubmitting(false)
      return
    }

    if (!selectedRoom || selectedRoom.id !== uniqueMatchedRoom?.id) {
      setSubmitError('请先确认当前匹配到的房间，再提交抄表。')
      setSubmitting(false)
      return
    }

    if (Number(form.currReading) < Number(form.prevReading)) {
      setSubmitError('本次读数不能小于上次读数。')
      setSubmitting(false)
      return
    }

    try {
      const result = await createWaterReading({
        roomId: selectedRoom.id,
        year: Number(form.year),
        month: Number(form.month),
        prevReading: Number(form.prevReading),
        currReading: Number(form.currReading),
        readAt: new Date(form.readAt).toISOString(),
        remark: form.remark || undefined,
      })
      setSubmitResult(`房间 ${selectedRoom?.buildingNo ?? ''}-${selectedRoom?.unitNo ?? ''}-${selectedRoom?.roomNo ?? ''} 的 ${formatPeriod(form.year, form.month, 'MONTH')} 抄表已录入，并已生成水费账单 ${result.billNo}。`)
      await loadData()
      setForm((current) => ({ ...initialForm, year: current.year, month: current.month }))
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : '抄表录入失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(item: WaterReading) {
    if (item.status !== 'NORMAL' && item.status !== 'ABNORMAL') {
      setError('当前抄表记录状态不可删除。')
      return
    }
    if (!window.confirm(`确认删除 ${item.roomLabel} 的 ${formatPeriod(item.periodYear, item.periodMonth)} 抄表记录吗？未支付关联水费账单会一并作废。`)) {
      return
    }
    setDeletingId(item.id)
    setError('')
    setSubmitResult('')
    try {
      await deleteWaterReading(item.id)
      setSubmitResult('抄表记录已删除，未支付关联水费账单已一并作废。')
      await loadData(filters)
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除抄表记录失败')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="space-y-6 pb-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">水费抄表</div>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">按房间条件录入抄表</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">先选小区，再输入楼号、单元号和户号定位房间。</p>
          </div>
          <button type="button" className="btn-secondary gap-2 whitespace-nowrap" onClick={() => void loadData()} disabled={loading}>
            <RefreshCcw className="h-4 w-4" />
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>
      </section>

      <PageSection title="录入抄表" description="新增抄表后会出现在下方列表中。">
        <form className="grid gap-4" onSubmit={handleSubmit}>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block sm:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">所属小区</span>
              <select
                className="input"
                value={selectedCommunityId || ''}
                onChange={(event) => {
                  const nextCommunityId = event.target.value ? Number(event.target.value) : undefined
                  setSelectedCommunityId(nextCommunityId)
                  setRoomFilters(initialRoomFilters)
                  setRooms([])
                }}
                disabled={roomLoading}
              >
                <option value="">请选择小区</option>
                {communities.map((community) => (
                  <option key={community.id} value={community.id}>{community.name}</option>
                ))}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">楼号</span>
              <input className="input" value={roomFilters.buildingNo} onChange={(event) => setRoomFilters((current) => ({ ...current, buildingNo: event.target.value }))} placeholder="例如 1 栋" disabled={!selectedCommunityId || roomLoading} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">单元号</span>
              <input className="input" value={roomFilters.unitNo} onChange={(event) => setRoomFilters((current) => ({ ...current, unitNo: event.target.value }))} placeholder="例如 2 单元" disabled={!selectedCommunityId || roomLoading} />
            </label>
            <label className="block sm:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">户号</span>
              <input className="input" value={roomFilters.roomNoKeyword} onChange={(event) => setRoomFilters((current) => ({ ...current, roomNoKeyword: event.target.value }))} placeholder="手填具体户号，例如 1201" disabled={!selectedCommunityId || roomLoading} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">年份</span>
              <input className="input" type="number" min={2020} max={2100} value={form.year} onChange={(event) => setForm((current) => ({ ...current, year: Number(event.target.value) }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">月份</span>
              <select className="input" value={form.month} onChange={(event) => setForm((current) => ({ ...current, month: Number(event.target.value) }))}>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>{month} 月</option>
                ))}
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">上次读数</span>
              <input className="input" type="number" step="0.001" value={form.prevReading} onChange={(event) => setForm((current) => ({ ...current, prevReading: Number(event.target.value) }))} />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">本次读数</span>
              <input className="input" type="number" step="0.001" value={form.currReading} onChange={(event) => setForm((current) => ({ ...current, currReading: Number(event.target.value) }))} />
            </label>
            <label className="block sm:col-span-2">
              <span className="mb-2 block text-sm font-medium text-slate-700">抄表时间</span>
              <input className="input" type="datetime-local" value={form.readAt} onChange={(event) => setForm((current) => ({ ...current, readAt: event.target.value }))} />
            </label>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="text-sm font-medium text-slate-500">当前房间</div>
                <div className="mt-1 text-base font-semibold text-slate-950">
                  {selectedRoom
                    ? `${selectedRoom.buildingNo}-${selectedRoom.unitNo}-${selectedRoom.roomNo}`
                    : !roomFiltersReady
                      ? '请先补全楼号、单元号和户号'
                      : matchedRooms.length === 0
                        ? '未找到匹配房间'
                        : uniqueMatchedRoom
                          ? `${uniqueMatchedRoom.buildingNo}-${uniqueMatchedRoom.unitNo}-${uniqueMatchedRoom.roomNo}（待确认）`
                          : `匹配到 ${matchedRooms.length} 个房间，请继续收窄条件`}
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm font-medium text-slate-500">用量预览</div>
                <div className="mt-1 text-3xl font-semibold text-slate-950">{formatQuantity(usagePreview)}</div>
              </div>
            </div>
            {uniqueMatchedRoom && !selectedRoom ? (
              <div className="mt-3">
                <button
                  type="button"
                  className="btn-secondary whitespace-nowrap"
                  onClick={() => setForm((current) => ({ ...current, roomId: uniqueMatchedRoom.id }))}
                >
                  确认使用该房间
                </button>
              </div>
            ) : null}
          </div>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">备注</span>
            <textarea className="textarea" rows={4} value={form.remark} onChange={(event) => setForm((current) => ({ ...current, remark: event.target.value }))} placeholder="可填写现场异常说明、表具状态等。" />
          </label>

          {submitError ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">{submitError}</div> : null}
          {submitResult ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{submitResult}</div> : null}

          <button type="submit" className="btn-primary w-full whitespace-nowrap sm:w-auto" disabled={submitting || roomLoading}>
            {submitting ? '提交中...' : '录入抄表并触发出账'}
          </button>
        </form>
      </PageSection>

      <PageSection
        title="当期抄表记录"
        description="按账期查看。"
        action={
          <div className="flex flex-wrap gap-2">
            <input className="input w-28" type="number" min={2020} max={2100} value={filters.periodYear} onChange={(event) => setFilters((current) => ({ ...current, periodYear: Number(event.target.value) }))} />
            <select className="input w-24" value={filters.periodMonth} onChange={(event) => setFilters((current) => ({ ...current, periodMonth: Number(event.target.value) }))}>
              {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                <option key={month} value={month}>{month} 月</option>
              ))}
            </select>
            <button type="button" className="btn-secondary whitespace-nowrap" onClick={() => void loadData(filters)} disabled={loading}>查询</button>
          </div>
        }
      >
        <AsyncState loading={loading} error={error} empty={!list.length} emptyDescription="当前账期暂无抄表记录。">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="whitespace-nowrap px-4 py-3 font-medium">房间</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">账期</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">上次读数</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">本次读数</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">用量</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">抄表时间</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 last:border-0 hover:bg-white/50">
                    <td className="whitespace-nowrap px-4 py-4 font-medium text-slate-900">{item.roomLabel}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatPeriod(item.periodYear, item.periodMonth)}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatQuantity(item.prevReading)}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatQuantity(item.currReading)}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-900">{formatQuantity(item.usageAmount)}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-600">{formatDateTime(item.readAt)}</td>
                    <td className="whitespace-nowrap px-4 py-4"><StatusBadge value={item.status} /></td>
                    <td className="whitespace-nowrap px-4 py-4 text-right">
                      <button
                        type="button"
                        className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
                        onClick={() => void handleDelete(item)}
                        disabled={deletingId === item.id}
                      >
                        <Trash2 className="h-4 w-4" />
                        {deletingId === item.id ? '删除中' : '删除'}
                      </button>
                    </td>
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
