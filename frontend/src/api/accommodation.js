import { hostGet } from './adminClient'

export async function fetchAccommodationDetail(accommodationId) {
  if (!accommodationId) {
    return { ok: false, status: 400, data: null }
  }
  return hostGet(`/public/detail/${accommodationId}`)
}
