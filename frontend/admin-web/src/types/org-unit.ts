export interface OrgUnit {
  id: number
  tenantCode: string
  orgCode: string
  name: string
  parentId?: number
  parentName?: string
  communityId?: number
}
