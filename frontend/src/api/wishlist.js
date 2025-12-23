import { authenticatedRequest } from './authClient'

// 내 위시리스트 ID 목록 조회 (메인페이지용)
export const fetchWishlistIds = () => {
    return authenticatedRequest('/api/wishlist/accommodation-ids', { method: 'GET' })
}

// 내 위시리스트 상세 조회 (마이페이지용)
export const fetchMyWishlist = () => {
    return authenticatedRequest('/api/wishlist', { method: 'GET' })
}

// 위시리스트 추가
export const addWishlist = (accommodationsId) => {
    return authenticatedRequest('/api/wishlist', {
        method: 'POST',
        body: JSON.stringify({ accommodationsId })
    })
}

// 위시리스트 삭제
export const removeWishlist = (accommodationId) => {
    return authenticatedRequest(`/api/wishlist/${accommodationId}`, { method: 'DELETE' })
}
