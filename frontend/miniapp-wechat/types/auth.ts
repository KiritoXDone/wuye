export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  accountId: number
  accountType: string
  roles: string[]
  needResetPassword?: boolean
}

export interface WechatLoginPayload {
  code: string
  nickname?: string
  avatarUrl?: string
}
