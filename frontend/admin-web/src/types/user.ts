export interface AdminUser {
  id: number
  accountNo: string
  accountType: string
  username: string
  realName: string
  mobile?: string | null
  status: number
  lastLoginAt?: string | null
}

export interface AdminUserListQuery {
  accountType?: string
}

export interface AdminUserCreatePayload {
  username: string
  password: string
  realName: string
  mobile?: string
}

export interface AdminUserStatusPayload {
  status: '0' | '1'
}

export interface AdminUserPasswordResetPayload {
  newPassword: string
}
