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
  roles: string[]
  needResetPassword: boolean
}

export interface Profile {
  accountId: number
  accountType: string
  realName: string
  roles: string[]
  groupIds: number[]
}
