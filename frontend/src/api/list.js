import { hostGet } from './adminClient'

const normalizeDateParam = (value) => {
  if (!value) return null
  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) return null
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  const trimmed = String(value).trim()
  return trimmed ? trimmed : null
}

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

export async function fetchListBulk(themeIds = [], keyword = '') {
  const params = {}
  if (Array.isArray(themeIds) && themeIds.length) {
    params.themeIds = themeIds.join(',')
  }
  const normalizedKeyword = String(keyword ?? '').trim()
  if (normalizedKeyword) {
    params.keyword = normalizedKeyword
  }
  return hostGet('/public/list/bulk', params)
}

export async function searchList({
  themeIds = [],
  keyword = '',
  page = 0,
  size = 24,
  bounds = null,
  checkin = null,
  checkout = null,
  guestCount = null
} = {}) {
  const params = { page, size }
  if (Array.isArray(themeIds) && themeIds.length) {
    params.themeIds = themeIds.join(',')
  }
  const normalizedKeyword = String(keyword ?? '').trim()
  if (normalizedKeyword) {
    params.keyword = normalizedKeyword
  }
  const normalizedCheckin = normalizeDateParam(checkin)
  const normalizedCheckout = normalizeDateParam(checkout)
  if (normalizedCheckin) {
    params.checkin = normalizedCheckin
  }
  if (normalizedCheckout) {
    params.checkout = normalizedCheckout
  }
  if (Number.isFinite(guestCount) && guestCount > 0) {
    params.guestCount = String(guestCount)
  }
  if (bounds) {
    params.minLat = bounds.minLat
    params.maxLat = bounds.maxLat
    params.minLng = bounds.minLng
    params.maxLng = bounds.maxLng
  }
  return hostGet('/public/search', params)
}
