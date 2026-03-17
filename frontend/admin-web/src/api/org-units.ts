import request from '@/utils/request'
import type { OrgUnit } from '@/types/org-unit'

export function getOrgUnits() {
  return request.get<never, OrgUnit[]>('/admin/org-units')
}
