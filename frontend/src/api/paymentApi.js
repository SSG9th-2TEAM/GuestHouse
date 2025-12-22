// 결제 API 클라이언트
const baseURL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

/**
 * 결제 승인 요청
 * @param {Object} data - { paymentKey, orderId, amount }
 * @returns {Promise<Object>} - 결제 결과
 */
export async function confirmPayment(data) {
    const response = await fetch(`${baseURL}/payments/confirm`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })

    if (!response.ok) {
        const error = await response.json().catch(() => ({}))
        throw new Error(error.message || `결제 승인 실패: ${response.status}`)
    }

    return response.json()
}

/**
 * 토스페이먼츠 클라이언트 키 조회
 * @returns {Promise<string>} - 클라이언트 키
 */
export async function getClientKey() {
    const response = await fetch(`${baseURL}/payments/client-key`)

    if (!response.ok) {
        throw new Error(`클라이언트 키 조회 실패: ${response.status}`)
    }

    return response.text()
}

/**
 * 주문번호로 결제 조회
 * @param {string} orderId - 주문번호
 * @returns {Promise<Object>} - 결제 정보
 */
export async function getPaymentByOrderId(orderId) {
    const response = await fetch(`${baseURL}/payments/order/${orderId}`)

    if (!response.ok) {
        throw new Error(`결제 조회 실패: ${response.status}`)
    }

    return response.json()
}
