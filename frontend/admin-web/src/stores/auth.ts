import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getMyProfile, loginByPassword, postLogout } from '@/api/auth'
import { storage } from '@/utils/storage'
import type { LoginPayload, LoginResult, Profile } from '@/types/auth'

const ACCESS_TOKEN_KEY = 'wuye_admin_access_token'
const REFRESH_TOKEN_KEY = 'wuye_admin_refresh_token'
const LOGIN_INFO_KEY = 'wuye_admin_login_info'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(storage.get(ACCESS_TOKEN_KEY, ''))
  const refreshToken = ref(storage.get(REFRESH_TOKEN_KEY, ''))
  const loginInfo = ref<LoginResult | null>(storage.get<LoginResult | null>(LOGIN_INFO_KEY, null))
  const profile = ref<Profile | null>(null)
  const profileLoaded = ref(false)

  const hasToken = computed(() => Boolean(accessToken.value))

  function applySession(result: LoginResult) {
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    loginInfo.value = result
    storage.set(ACCESS_TOKEN_KEY, result.accessToken)
    storage.set(REFRESH_TOKEN_KEY, result.refreshToken)
    storage.set(LOGIN_INFO_KEY, result)
  }

  async function login(payload: LoginPayload) {
    const result = await loginByPassword(payload)
    applySession(result)
    await fetchProfile()
    return result
  }

  async function fetchProfile() {
    if (!hasToken.value) {
      profile.value = null
      profileLoaded.value = false
      return null
    }
    const result = await getMyProfile()
    profile.value = result
    profileLoaded.value = true
    return result
  }

  async function logout() {
    try {
      if (hasToken.value) {
        await postLogout()
      }
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    loginInfo.value = null
    profile.value = null
    profileLoaded.value = false
    storage.remove(ACCESS_TOKEN_KEY)
    storage.remove(REFRESH_TOKEN_KEY)
    storage.remove(LOGIN_INFO_KEY)
  }

  return {
    accessToken,
    refreshToken,
    loginInfo,
    profile,
    profileLoaded,
    hasToken,
    login,
    fetchProfile,
    logout,
    clearSession,
  }
})
