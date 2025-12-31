import { hostGet } from './adminClient'

export async function fetchList(themeIds = [], keyword = '') {
  const params = {}
  if (Array.isArray(themeIds) && themeIds.length) {
    params.themeIds = themeIds.join(',')
  }
  const normalizedKeyword = String(keyword ?? '').trim()
  if (normalizedKeyword) {
    params.keyword = normalizedKeyword
  }
  return hostGet('/public/list', params)
}

export async function searchList({ themeIds = [], keyword = '', page = 0, size = 24, bounds = null } = {}) {
  const params = { page, size }
  if (Array.isArray(themeIds) && themeIds.length) {
    params.themeIds = themeIds.join(',')
  }
  const normalizedKeyword = String(keyword ?? '').trim()
  if (normalizedKeyword) {
    params.keyword = normalizedKeyword
  }
  if (bounds) {
    params.minLat = bounds.minLat
    params.maxLat = bounds.maxLat
    params.minLng = bounds.minLng
    params.maxLng = bounds.maxLng
  }
  return hostGet('/public/search', params)
}
