import { hostGet } from './adminClient'

/**
 * 사용자 맞춤 추천 숙소 조회
 * @param {number} userId - 사용자 ID
 * @param {number} limit - 추천 개수 (기본값: 10)
 * @returns {Promise} 추천 숙소 목록
 */
export async function fetchRecommendations(userId, limit = 10) {
    return hostGet(`/recommendations?userId=${userId}&limit=${limit}`)
}
