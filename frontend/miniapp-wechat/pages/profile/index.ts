import {
  bindMyRoom,
  getMyRooms,
  getRoomBindBuildings,
  getRoomBindCommunities,
  getRoomBindRooms,
  getRoomBindUnits,
  unbindMyRoom,
} from '../../services/room'
import type { BuildingOption, CommunityOption, RoomItem, RoomOption, UnitOption } from '../../types/room'
import { clearAuthSession, hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatMoney } from '../../utils/format'

Page({
  data: {
    loading: true,
    bindSubmitting: false,
    errorMessage: '',
    successMessage: '',
    rooms: [] as Array<RoomItem & { statusLabel: string; areaText: string }>,
    communities: [] as CommunityOption[],
    buildings: [] as BuildingOption[],
    units: [] as UnitOption[],
    roomOptions: [] as RoomOption[],
    selectedCommunityIndex: 0,
    selectedBuildingIndex: 0,
    selectedUnitIndex: 0,
    selectedRoomIndex: 0,
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.loadProfileData()
  },

  async loadProfileData() {
    this.setData({ loading: true, errorMessage: '', successMessage: '' })
    try {
      const [rooms, communities] = await Promise.all([getMyRooms(), getRoomBindCommunities()])
      const activeRooms = rooms
        .filter((item) => item.bindingStatus === 'ACTIVE')
        .map((item) => ({
          ...item,
          statusLabel: formatBillStatus(item.bindingStatus),
          areaText: formatMoney(item.areaM2),
        }))
      this.setData({ rooms: activeRooms, communities })
      if (communities.length) {
        await this.loadBuildings(communities[0].communityId)
      }
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '我的页面加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadBuildings(communityId: number) {
    const buildings = await getRoomBindBuildings(communityId)
    this.setData({ buildings, selectedBuildingIndex: 0, units: [], roomOptions: [], selectedUnitIndex: 0, selectedRoomIndex: 0 })
    if (buildings.length) {
      await this.loadUnits(communityId, buildings[0].buildingNo)
    }
  },

  async loadUnits(communityId: number, buildingNo: string) {
    const units = await getRoomBindUnits(communityId, buildingNo)
    this.setData({ units, selectedUnitIndex: 0, roomOptions: [], selectedRoomIndex: 0 })
    if (units.length) {
      await this.loadRooms(communityId, buildingNo, units[0].unitNo)
    }
  },

  async loadRooms(communityId: number, buildingNo: string, unitNo: string) {
    const roomOptions = await getRoomBindRooms(communityId, buildingNo, unitNo)
    this.setData({ roomOptions, selectedRoomIndex: 0 })
  },

  async handleCommunityChange(event: WechatMiniprogram.CustomEvent) {
    const selectedCommunityIndex = Number(event.detail.value || 0)
    const community = this.data.communities[selectedCommunityIndex]
    this.setData({ selectedCommunityIndex })
    if (community) {
      await this.loadBuildings(community.communityId)
    }
  },

  async handleBuildingChange(event: WechatMiniprogram.CustomEvent) {
    const selectedBuildingIndex = Number(event.detail.value || 0)
    const community = this.data.communities[this.data.selectedCommunityIndex]
    const building = this.data.buildings[selectedBuildingIndex]
    this.setData({ selectedBuildingIndex })
    if (community && building) {
      await this.loadUnits(community.communityId, building.buildingNo)
    }
  },

  async handleUnitChange(event: WechatMiniprogram.CustomEvent) {
    const selectedUnitIndex = Number(event.detail.value || 0)
    const community = this.data.communities[this.data.selectedCommunityIndex]
    const building = this.data.buildings[this.data.selectedBuildingIndex]
    const unit = this.data.units[selectedUnitIndex]
    this.setData({ selectedUnitIndex })
    if (community && building && unit) {
      await this.loadRooms(community.communityId, building.buildingNo, unit.unitNo)
    }
  },

  handleRoomChange(event: WechatMiniprogram.CustomEvent) {
    this.setData({ selectedRoomIndex: Number(event.detail.value || 0) })
  },

  async handleBindRoom() {
    const room = this.data.roomOptions[this.data.selectedRoomIndex]
    if (!room) {
      this.setData({ errorMessage: '请先完成房间选择。', successMessage: '' })
      return
    }
    this.setData({ bindSubmitting: true, errorMessage: '', successMessage: '' })
    try {
      const result = await bindMyRoom({ roomId: room.roomId })
      this.setData({ successMessage: `${result.roomLabel} 已绑定成功。` })
      await this.loadProfileData()
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '绑定房间失败' })
    } finally {
      this.setData({ bindSubmitting: false })
    }
  },

  async handleUnbindRoom(event: WechatMiniprogram.BaseEvent) {
    const roomId = Number(event.currentTarget.dataset.roomId || 0)
    const roomLabel = String(event.currentTarget.dataset.roomLabel || '')
    if (!roomId) {
      return
    }
    try {
      await unbindMyRoom(roomId)
      this.setData({ successMessage: `${roomLabel} 已解绑。`, errorMessage: '' })
      await this.loadProfileData()
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '解绑失败', successMessage: '' })
    }
  },

  openRewards() {
    wx.navigateTo({ url: '/pages/rewards/index' })
  },

  openAgent() {
    wx.navigateTo({ url: '/pages/agent/index' })
  },

  handleLogout() {
    clearAuthSession()
    wx.reLaunch({ url: '/pages/login/index' })
  }
})
