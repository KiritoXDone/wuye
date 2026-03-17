import { API_BASE_URL, DEFAULT_DEV_LOGIN_CODE, QUICK_DEV_LOGIN_CODES } from '../../config/env'
import { loginWechat } from '../../services/auth'
import { hasAuthSession, setAuthSession } from '../../utils/auth'

Page({
  data: {
    code: DEFAULT_DEV_LOGIN_CODE,
    quickCodes: QUICK_DEV_LOGIN_CODES,
    baseUrl: API_BASE_URL,
    submitting: false,
    wxCodeLoading: false,
    errorMessage: ''
  },

  onShow() {
    if (hasAuthSession()) {
      wx.reLaunch({ url: '/pages/rooms/index' })
    }
  },

  handleCodeInput(event: WechatMiniprogram.Input) {
    this.setData({ code: event.detail.value, errorMessage: '' })
  },

  handleQuickFill(event: WechatMiniprogram.BaseEvent) {
    this.setData({ code: event.currentTarget.dataset.code, errorMessage: '' })
  },

  fillWechatCode() {
    this.setData({ wxCodeLoading: true, errorMessage: '' })
    wx.login({
      success: (res) => {
        this.setData({ code: res.code || DEFAULT_DEV_LOGIN_CODE })
        if (res.code) {
          wx.showToast({ title: '已填入微信 code', icon: 'none' })
        }
      },
      fail: () => {
        this.setData({ errorMessage: '未能获取微信 code，请继续使用本地演示 code 联调。' })
      },
      complete: () => {
        this.setData({ wxCodeLoading: false })
      }
    })
  },

  async handleLogin() {
    if (!this.data.code || this.data.submitting) {
      return
    }

    this.setData({ submitting: true, errorMessage: '' })

    try {
      const result = await loginWechat({
        code: this.data.code,
        nickname: '住户用户'
      })
      setAuthSession(result)
      wx.reLaunch({ url: '/pages/rooms/index' })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '登录失败，请稍后再试' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
