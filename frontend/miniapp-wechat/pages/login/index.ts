import { DEFAULT_DEV_LOGIN_CODE } from '../../config/env'
import { loginWechat } from '../../services/auth'
import { hasAuthSession, setAuthSession } from '../../utils/auth'

Page({
  data: {
    submitting: false,
    errorMessage: ''
  },

  onShow() {
    if (hasAuthSession()) {
      wx.reLaunch({ url: '/pages/rooms/index' })
    }
  },

  async handleLogin() {
    if (this.data.submitting) {
      return
    }

    this.setData({ submitting: true, errorMessage: '' })

    try {
      const code = await this.fetchWechatCode()
      const result = await loginWechat({
        code,
        nickname: '住户用户'
      })
      setAuthSession(result)
      wx.reLaunch({ url: '/pages/rooms/index' })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '登录失败，请稍后再试' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  fetchWechatCode() {
    return new Promise<string>((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            resolve(res.code)
            return
          }
          resolve(DEFAULT_DEV_LOGIN_CODE)
        },
        fail: () => {
          reject(new Error('未能获取微信登录凭证'))
        }
      })
    })
  }
})
