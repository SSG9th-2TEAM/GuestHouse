import { hostGet } from './adminClient'

export async function fetchHostReviews(params = {}) {
  return hostGet('/host/reviews', params)
}

export async function fetchHostReviewSummary() {
  return hostGet('/host/reviews/summary')
}
