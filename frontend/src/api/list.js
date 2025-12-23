import { hostGet } from './adminClient'

export async function fetchList(themeIds = []) {
  const params = {}
  if (Array.isArray(themeIds) && themeIds.length) {
    params.themeIds = themeIds.join(',')
  }
  return hostGet('/public/list', params)
}
