<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const reservation = ref({
  id: route.params.id,
  hotelName: '그랜드 호텔 서울',
  location: '서울시 강남구',
  dates: '2025년 12월 12일 ~ 12월 13일',
  guests: 1,
  price: 150000,
  image: 'https://picsum.photos/id/11/200/200'
})

onMounted(() => {
  if (history.state && history.state.reservationData) {
    reservation.value = { ...reservation.value, ...history.state.reservationData }
  }
})

const cancelReason = ref('')
const agreed = ref(false)

// Modal State
const showModal = ref(false)
const modalMessage = ref('')
const modalType = ref('info')
const modalCallback = ref(null)

const openModal = (message, type = 'info', callback = null) => {
  modalMessage.value = message
  modalType.value = type
  modalCallback.value = callback
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (modalCallback.value) {
    modalCallback.value()
    modalCallback.value = null
  }
}

const handleCancel = () => {
  if (!cancelReason.value.trim()) {
    openModal('환불 사유를 입력해주세요.', 'error')
    return
  }
  if (!agreed.value) {
    openModal('환불 규정에 동의해주세요.', 'error')
    return
  }
  
  openModal('환불 요청이 완료되었습니다.', 'success', () => router.push('/reservations'))
}
</script>

<template>
  <div class="cancel-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>예약 취소</h1>
    </div>

    <!-- Info Card -->
    <div class="info-card">
      <img :src="reservation.image" class="info-img" />
      <div class="info-content">
        <h3>{{ reservation.hotelName }}</h3>
        <p class="loc">{{ reservation.location }}</p>
        <p class="date">{{ reservation.dates }}</p>
        <p class="guests">게스트 {{ reservation.guests }}명</p>
      </div>
    </div>

    <!-- Refund Guide -->
    <div class="refund-guide">
      <h3>환불 안내</h3>
      <ul>
        <li>체크인 7일 전: 100% 환불</li>
        <li>체크인 3일 전: 50% 환불</li>
        <li>체크인 3일 이내: 환불 불가</li>
      </ul>
      <div class="refund-amount">
        <span>예상 환불 금액</span>
        <span class="amount">₩{{ reservation.price?.toLocaleString() }}</span>
      </div>
    </div>

    <!-- Cancel Reason -->
    <div class="reason-section">
      <h3>취소 사유</h3>
      <textarea v-model="cancelReason" placeholder="취소 사유를 입력해주세요."></textarea>
    </div>

    <!-- Refund Method -->
    <div class="method-section">
      <h3>환불 수단</h3>
      <div class="method-box">
        <span>💳</span>
        <span>신용카드 (****-1234)</span>
      </div>
    </div>

    <!-- Warning -->
    <div class="warning-box">
      <p>⚠️ 취소 후에는 되돌릴 수 없습니다.</p>
    </div>

    <!-- Agreement -->
    <label class="agreement">
      <input type="checkbox" v-model="agreed" />
      <span>위 환불 규정을 확인하고 동의합니다.</span>
    </label>

    <!-- Bottom Bar -->
    <div class="bottom-bar">
      <button class="cancel-btn outline" @click="router.back()">뒤로가기</button>
      <button class="cancel-btn primary" @click="handleCancel">환불 요청</button>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-icon" :class="modalType">
          <span v-if="modalType === 'success'">✓</span>
          <span v-else-if="modalType === 'error'">!</span>
          <span v-else>i</span>
        </div>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cancel-page {
  padding-top: 1rem;
  padding-bottom: 120px;
  max-width: 600px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-header h1 {
  font-size: 1.2rem;
  font-weight: 700;
}

/* Info Card */
.info-card {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 12px;
  margin-bottom: 1.5rem;
}

.info-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.info-content h3 {
  font-size: 1rem;
  margin-bottom: 0.3rem;
}

.info-content .loc {
  font-size: 0.85rem;
  color: #666;
}

.info-content .date,
.info-content .guests {
  font-size: 0.85rem;
  color: #888;
}

/* Refund Guide */
.refund-guide {
  padding: 1.2rem;
  border: 1px solid #eee;
  border-radius: 12px;
  margin-bottom: 1.5rem;
}

.refund-guide h3 {
  font-size: 0.95rem;
  margin-bottom: 0.8rem;
}

.refund-guide ul {
  list-style: disc inside;
  font-size: 0.85rem;
  color: #555;
  line-height: 1.6;
  margin-bottom: 1rem;
}

.refund-amount {
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  padding-top: 0.8rem;
  border-top: 1px solid #eee;
}

.amount {
  color: #2563eb;
}

/* Reason */
.reason-section {
  margin-bottom: 1.5rem;
}

.reason-section h3 {
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.reason-section textarea {
  width: 100%;
  height: 100px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1rem;
  font-size: 0.9rem;
  resize: none;
  outline: none;
}

.reason-section textarea:focus {
  border-color: var(--primary);
}

/* Method */
.method-section {
  margin-bottom: 1.5rem;
}

.method-section h3 {
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.method-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #f9f9f9;
  font-size: 0.9rem;
}

/* Warning */
.warning-box {
  padding: 1rem;
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.warning-box p {
  font-size: 0.9rem;
  color: #b91c1c;
}

/* Agreement */
.agreement {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  cursor: pointer;
  margin-bottom: 2rem;
}

.agreement input {
  width: 18px;
  height: 18px;
}

/* Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1rem;
  background: white;
  border-top: 1px solid #eee;
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.cancel-btn {
  flex: 1;
  max-width: 280px;
  padding: 1rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
}

.cancel-btn.outline {
  background: white;
  border: 1px solid #ddd;
  color: #333;
}

.cancel-btn.primary {
  background: var(--primary);
  color: #004d40;
  border: none;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 320px;
  width: 90%;
  text-align: center;
}

.modal-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 1.5rem;
  font-weight: bold;
}

.modal-icon.success {
  background: var(--primary);
  color: #004d40;
}

.modal-icon.error {
  background: #fee2e2;
  color: #dc2626;
}

.modal-icon.info {
  background: #e0f2fe;
  color: #0284c7;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 0.8rem;
  background: var(--primary);
  color: #004d40;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
</style>
