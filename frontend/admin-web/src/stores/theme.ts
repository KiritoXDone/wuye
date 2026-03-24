import { create } from 'zustand'

import { storage } from '@/utils/storage'

export type ThemeMode = 'system' | 'light' | 'dark'
export type ResolvedTheme = 'light' | 'dark'

const THEME_MODE_KEY = 'wuye_admin_theme_mode'

let mediaQuery: MediaQueryList | null = null
let removeSystemListener: (() => void) | null = null

function getSystemTheme(): ResolvedTheme {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function resolveTheme(mode: ThemeMode): ResolvedTheme {
  return mode === 'system' ? getSystemTheme() : mode
}

function applyTheme(resolvedTheme: ResolvedTheme) {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.classList.toggle('dark', resolvedTheme === 'dark')
  document.documentElement.dataset.theme = resolvedTheme
}

const initialMode = typeof window === 'undefined' ? 'system' : storage.get<ThemeMode>(THEME_MODE_KEY, 'system')
const initialResolvedTheme = resolveTheme(initialMode)

interface ThemeState {
  mode: ThemeMode
  resolvedTheme: ResolvedTheme
  initialized: boolean
  init: () => void
  setMode: (mode: ThemeMode) => void
  syncSystemTheme: () => void
}

applyTheme(initialResolvedTheme)

export const useThemeStore = create<ThemeState>((set, get) => ({
  mode: initialMode,
  resolvedTheme: initialResolvedTheme,
  initialized: false,
  init: () => {
    if (get().initialized || typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return
    }
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handleChange = () => get().syncSystemTheme()
    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', handleChange)
      removeSystemListener = () => mediaQuery?.removeEventListener('change', handleChange)
    } else {
      mediaQuery.addListener(handleChange)
      removeSystemListener = () => mediaQuery?.removeListener(handleChange)
    }
    set({ initialized: true })
    get().syncSystemTheme()
  },
  setMode: (mode) => {
    storage.set(THEME_MODE_KEY, mode)
    const resolvedTheme = resolveTheme(mode)
    applyTheme(resolvedTheme)
    set({ mode, resolvedTheme })
  },
  syncSystemTheme: () => {
    const { mode } = get()
    const resolvedTheme = resolveTheme(mode)
    applyTheme(resolvedTheme)
    set({ resolvedTheme })
  },
}))

export function disposeThemeStore() {
  removeSystemListener?.()
  removeSystemListener = null
  mediaQuery = null
}
