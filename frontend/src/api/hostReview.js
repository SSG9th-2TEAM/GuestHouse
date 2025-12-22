import { hostGet, hostRequest } from './adminClient'

export async function fetchHostReviews(params = {}) {
  return hostGet('/host/review', params)
}

export async function createHostReviewReply(reviewId, payload) {
  return hostRequest(`/host/review/${reviewId}/reply`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function reportHostReview(reviewId, payload) {
  return hostRequest(`/host/review/${reviewId}/report`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}
