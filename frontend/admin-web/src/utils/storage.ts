export const storage = {
  get<T>(key: string, fallback: T): T {
    const value = window.localStorage.getItem(key)
    if (!value) {
      return fallback
    }
    try {
      return JSON.parse(value) as T
    } catch {
      return fallback
    }
  },
  set(key: string, value: unknown) {
    window.localStorage.setItem(key, JSON.stringify(value))
  },
  remove(key: string) {
    window.localStorage.removeItem(key)
  },
}
