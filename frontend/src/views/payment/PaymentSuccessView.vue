<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { confirmPayment } from '@/api/paymentApi'

const router = useRouter()
const route = useRoute()

const isLoading = ref(true)
const isSuccess = ref(false)
const errorMessage = ref('')
const paymentResult = ref(null)

onMounted(async () => {
  const { paymentKey, orderId, amount, reservationId } = route.query

  // 필수 파라미터 없으면 홈으로
  if (!paymentKey || !orderId || !amount) {
    errorMessage.value = '결제 정보가 올바르지 않습니다.'
    isLoading.value = false
    // 3초 후 홈으로 이동
    setTimeout(() => router.replace('/'), 3000)
    return
  }

  try {
    // 결제 승인 API 호출
    const result = await confirmPayment({
      paymentKey,
      orderId,
      amount: parseInt(amount)
    })

    paymentResult.value = result
    isSuccess.value = true

    // 2초 후 예약 완료 페이지로 이동 (replace 사용하여 뒤로가기 방지)
    setTimeout(() => {
      router.replace({
        name: 'booking-success',
        state: {
          bookingData: {
            reservationId: reservationId,
            paymentId: result.paymentId,
            totalPrice: result.approvedAmount
          }
        }
      })
    }, 2000)

  } catch (error) {
    console.error('결제 승인 실패:', error)
    
    // 이미 처리된 결제인 경우 (중복 요청)
    if (error.message?.includes('이미') || error.message?.includes('중복')) {
      errorMessage.value = '이미 처리된 결제입니다. 예약 내역을 확인해주세요.'
    } else {
      errorMessage.value = error.message || '결제 승인에 실패했습니다.'
    }
    
    // 3초 후 홈으로 이동
    setTimeout(() => router.replace('/'), 3000)
  } finally {
    isLoading.value = false
  }
})

// 결제 수단 한글 변환
const getPaymentMethodName = (method) => {
  const methodNames = {
    '카드': '신용/체크카드',
    'CARD': '신용/체크카드',
    '가상계좌': '가상계좌',
    'VIRTUAL_ACCOUNT': '가상계좌',
    '계좌이체': '계좌이체',
    'TRANSFER': '계좌이체',
    '휴대폰': '휴대폰 결제',
    'MOBILE_PHONE': '휴대폰 결제',
    '간편결제': '간편결제',
    'EASY_PAY': '간편결제',
    '토스페이': '토스페이',
    '토스결제': '토스페이'
  }
  return methodNames[method] || method || '카드'
}

const goHome = () => router.replace('/')
</script>

<template>
  <div class="payment-result-page">
    <!-- 로딩 -->
    <div v-if="isLoading" class="loading-container">
      <div class="spinner"></div>
      <p>결제를 확인하고 있습니다...</p>
    </div>

    <!-- 성공 -->
    <div v-else-if="isSuccess" class="success-container">
      <div class="check-icon">✓</div>
      <h1>결제가 완료되었습니다!</h1>
      <p>잠시 후 예약 완료 페이지로 이동합니다...</p>
      <div class="payment-info" v-if="paymentResult">
        <div class="info-row">
          <span>결제 금액</span>
          <span>₩{{ paymentResult.approvedAmount?.toLocaleString() }}</span>
        </div>
        <div class="info-row">
          <span>결제 수단</span>
          <span>{{ getPaymentMethodName(paymentResult.paymentMethod) }}</span>
        </div>
      </div>
    </div>

    <!-- 실패 -->
    <div v-else class="error-container">
      <div class="error-icon">✕</div>
      <h1>결제 승인에 실패했습니다</h1>
      <p class="error-message">{{ errorMessage }}</p>
      <button class="btn" @click="goHome">홈으로 돌아가기</button>
    </div>
  </div>
</template>

<style scoped>
.payment-result-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.loading-container,
.success-container,
.error-container {
  text-align: center;
  padding: 2rem;
  max-width: 400px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e0e0e0;
  border-top-color: #0064FF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.check-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  color: white;
  border-radius: 50%;
  font-size: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.error-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  color: white;
  border-radius: 50%;
  font-size: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

h1 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #333;
}

p {
  color: #666;
  margin-bottom: 1.5rem;
}

.payment-info {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-top: 1.5rem;
  text-align: left;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  font-size: 0.95rem;
}

.error-message {
  color: #e11d48;
}

.btn {
  background: #0064FF;
  color: white;
  padding: 0.8rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
}
</style>
