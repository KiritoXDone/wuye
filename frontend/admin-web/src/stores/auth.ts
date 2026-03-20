import { create } from 'zustand'

import { getMyProfile, loginByPassword, postLogout } from '@/api/auth'
import type { LoginPayload, LoginResult, Profile } from '@/types/auth'
import { storage } from '@/utils/storage'

const ACCESS_TOKEN_KEY = 'wuye_admin_access_token'
const REFRESH_TOKEN_KEY = 'wuye_admin_refresh_token'
const LOGIN_INFO_KEY = 'wuye_admin_login_info'

interface AuthState {
  accessToken: string
  refreshToken: string
  loginInfo: LoginResult | null
  profile: Profile | null
  profileLoaded: boolean
  hasToken: boolean
  applySession: (result: LoginResult) => void
  login: (payload: LoginPayload) => Promise<LoginResult>
  fetchProfile: () => Promise<Profile | null>
  logout: () => Promise<void>
  clearSession: () => void
}

const initialAccessToken = storage.get(ACCESS_TOKEN_KEY, '')
const initialRefreshToken = storage.get(REFRESH_TOKEN_KEY, '')
const initialLoginInfo = storage.get<LoginResult | null>(LOGIN_INFO_KEY, null)

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: initialAccessToken,
  refreshToken: initialRefreshToken,
  loginInfo: initialLoginInfo,
  profile: null,
  profileLoaded: false,
  hasToken: Boolean(initialAccessToken),
  applySession: (result) => {
    storage.set(ACCESS_TOKEN_KEY, result.accessToken)
    storage.set(REFRESH_TOKEN_KEY, result.refreshToken)
    storage.set(LOGIN_INFO_KEY, result)
    set({
      accessToken: result.accessToken,
      refreshToken: result.refreshToken,
      loginInfo: result,
      profileLoaded: false,
      hasToken: Boolean(result.accessToken),
    })
  },
  login: async (payload) => {
    const result = await loginByPassword(payload)
    get().applySession(result)
    await get().fetchProfile()
    return result
  },
  fetchProfile: async () => {
    if (!get().accessToken) {
      set({ profile: null, profileLoaded: false, hasToken: false })
      return null
    }
    const result = await getMyProfile()
    set({ profile: result, profileLoaded: true, hasToken: true })
    return result
  },
  logout: async () => {
    try {
      if (get().accessToken) {
        await postLogout()
      }
    } finally {
      get().clearSession()
    }
  },
  clearSession: () => {
    storage.remove(ACCESS_TOKEN_KEY)
    storage.remove(REFRESH_TOKEN_KEY)
    storage.remove(LOGIN_INFO_KEY)
    set({
      accessToken: '',
      refreshToken: '',
      loginInfo: null,
      profile: null,
      profileLoaded: false,
      hasToken: false,
    })
  },
}))
