import { useEffect } from 'react'

import { useThemeStore } from '@/stores/theme'

export default function ThemeController() {
  const init = useThemeStore((state) => state.init)

  useEffect(() => {
    init()
  }, [init])

  return null
}
