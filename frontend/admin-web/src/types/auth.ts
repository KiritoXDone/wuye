export interface LoginPayload {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  accountId: number
  accountType: string
  productRole: 'ADMIN' | 'USER'
  roles: string[]
  needResetPassword: boolean
}

export interface Profile {
  accountId: number
  accountType: string
  productRole: 'ADMIN' | 'USER'
  realName: string
  roles: string[]
}
