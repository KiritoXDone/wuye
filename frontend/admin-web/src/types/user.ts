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

export interface AdminUserStatusUpdatePayload {
  status: '0' | '1'
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

export interface AdminUserPasswordResetPayload {
  newPassword: string
}

export interface AdminUserRoom {
  roomId: number
  roomLabel: string
  communityName?: string
  areaM2?: number | string
  bindingStatus?: string
}
