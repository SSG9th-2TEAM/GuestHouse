// 예약 API 클라이언트
const baseURL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

/**
 * 예약 생성
 * @param {Object} data - 예약 요청 데이터
 * @returns {Promise<Object>} - 생성된 예약 정보
 */
export async function createReservation(data) {
    const response = await fetch(`${baseURL}/reservations`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })

    if (!response.ok) {
        throw new Error(`예약 생성 실패: ${response.status}`)
    }

    return response.json()
}

/**
 * 예약 단건 조회
 * @param {number} reservationId - 예약 ID
 * @returns {Promise<Object>} - 예약 정보
 */
export async function getReservation(reservationId) {
    const response = await fetch(`${baseURL}/reservations/${reservationId}`)

    if (!response.ok) {
        throw new Error(`예약 조회 실패: ${response.status}`)
    }

    return response.json()
}

/**
 * 사용자별 예약 목록 조회
 * @param {number} userId - 사용자 ID (없으면 기본값 1 사용)
 * @returns {Promise<Array>} - 예약 목록
 */
export async function getUserReservations(userId = 1) {
    const response = await fetch(`${baseURL}/reservations/user/${userId}`)

    if (!response.ok) {
        throw new Error(`예약 목록 조회 실패: ${response.status}`)
    }

    return response.json()
}

/**
 * 숙소별 예약 목록 조회
 * @param {number} accommodationsId - 숙소 ID
 * @returns {Promise<Array>} - 예약 목록
 */
export async function getAccommodationReservations(accommodationsId) {
    const response = await fetch(`${baseURL}/reservations/accommodation/${accommodationsId}`)

    if (!response.ok) {
        throw new Error(`숙소 예약 목록 조회 실패: ${response.status}`)
    }

    return response.json()
}

/**
 * 대기 상태 예약 삭제 (결제 취소 시)
 * @param {number} reservationId - 예약 ID
 * @returns {Promise<void>}
 */
export async function deletePendingReservation(reservationId) {
    const response = await fetch(`${baseURL}/reservations/pending/${reservationId}`, {
        method: 'DELETE'
    })

    if (!response.ok) {
        console.error(`대기 예약 삭제 실패: ${response.status}`)
    }
}
