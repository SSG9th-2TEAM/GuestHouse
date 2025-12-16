<script setup>
import { useRouter } from 'vue-router'
import { onMounted, ref } from 'vue'

const router = useRouter()

// Default fallback data if accessed directly
const info = ref({
  bookingNumber: 'BK' + Math.floor(Math.random() * 100000000),
  hotelName: '그랜드 호텔 서울',
  location: '서울 강남구 테헤란로 123',
  dates: '2025-12-08 ~ 2025-12-11',
  nights: 3,
  guests: '성인 2명',
  basePrice: 450000,
  fees: 0,
  totalPrice: 450000,
  paymentDate: new Date().toLocaleString()
})

onMounted(() => {
  const state = history.state
  if (state && state.bookingData) {
    const data = state.bookingData
    info.value = {
      ...info.value,
      hotelName: data.hotelName,
      location: '서울 종로구 삼청로 45', // Mock location as it wasn't in booking obj explicitly
      dates: data.dates,
      guests: data.guests,
      basePrice: data.basePrice, 
      totalPrice: data.totalPrice,
      paymentDate: new Date().toLocaleString()
    }
  }
})

const goHome = () => router.push('/')
const goHistory = () => router.push('/reservations')
</script>

<template>
  <div class="success-page">
    <!-- Top Banner -->
    <div class="top-banner">
      <div class="check-icon">✓</div>
      <h1>예약이 완료되었습니다!</h1>
      <p>예약 확인 메일이 발송되었습니다</p>
    </div>

    <div class="container content">
      <!-- Booking Number -->
      <div class="card booking-number-card">
        <label>예약 번호</label>
        <div class="number">{{ info.bookingNumber }}</div>
      </div>

      <!-- Reservation Info -->
      <div class="card info-card">
        <h3>예약 정보</h3>
        <hr class="divider"/>
        
        <div class="info-item">
          <div class="text">
            <span class="label">숙소</span>
            <span class="value">{{ info.hotelName }}</span>
          </div>
        </div>

        <div class="info-item">
          <div class="text">
            <span class="label">위치</span>
            <span class="value">{{ info.location }}</span>
          </div>
        </div>

        <div class="info-item">
          <div class="text">
            <span class="label">체크인 / 체크아웃</span>
            <span class="value">{{ info.dates }}</span>
            <span class="sub-text">{{ info.nights || '1' }}박</span>
          </div>
        </div>

        <div class="info-item">
          <div class="text">
            <span class="label">투숙객</span>
            <span class="value">{{ info.guests }}</span>
          </div>
        </div>
      </div>

      <!-- Payment Info -->
      <div class="card payment-card">
        <h3>결제 정보</h3>
        <hr class="divider"/>
        
        <div class="row">
          <span>숙박 요금</span>
          <span>{{ info.basePrice?.toLocaleString() }}원</span>
        </div>
        <div class="row">
          <span>수수료</span>
          <span>{{ info.fees }}원</span>
        </div>
        <hr class="divider-light"/>
        <div class="row total">
          <span>총 결제 금액</span>
          <span>{{ info.totalPrice?.toLocaleString() }}원</span>
        </div>

        <div class="payment-date">
          결제 완료<br/>
          {{ info.paymentDate }}
        </div>
      </div>

      <!-- Guide -->
      <div class="card guide-card">
        <h3>체크인 안내</h3>
        <ul>
          <li>체크인 시간: 오후 3시 이후</li>
          <li>체크아웃 시간: 오전 11시</li>
          <li>신분증을 지참해 주세요</li>
        </ul>
      </div>

      <!-- Buttons -->
      <div class="actions">
        <button class="primary-btn" @click="goHistory">예약 내역 보기</button>
        <button class="secondary-btn" @click="goHome">새로운 예약하기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.success-page {
  min-height: 100vh;
  background-color: var(--bg-body);
  padding-bottom: 2rem;
}

.top-banner {
  background-color: var(--primary);
  color: #004d40;
  padding: 3rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.check-icon {
  width: 60px;
  height: 60px;
  border: 4px solid #004d40;
  border-radius: 50%;
  font-size: 2rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.top-banner h1 {
  font-size: 1.5rem;
  margin: 0;
}

.top-banner p {
  color: #00695c;
  font-size: 0.9rem;
}

.content {
  max-width: 500px; /* Narrower for mobile-like feel per screenshot */
  margin: -1.5rem auto 0;
  padding: 0 1rem;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  border: 1px solid #eee;
}

.booking-number-card {
  text-align: center;
  padding: 1.5rem;
}
.booking-number-card label {
  color: #888;
  font-size: 0.85rem;
  display: block;
  margin-bottom: 0.25rem;
}
.booking-number-card .number {
  font-weight: bold;
  font-size: 1.1rem;
  letter-spacing: 1px;
}

h3 {
  font-size: 1rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}
.divider {
  border: 0;
  border-top: 2px solid #f5f5f5;
  margin: 0.8rem 0 1.2rem;
}
.divider-light {
  border: 0;
  border-top: 1px solid #eee;
  margin: 0.8rem 0;
}

.info-item {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.2rem;
}
.icon {
  color: #999;
  font-size: 1.1rem;
}
.text {
  display: flex;
  flex-direction: column;
}
.label {
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 2px;
}
.value {
  font-size: 0.95rem;
  font-weight: 500;
  color: #333;
}
.sub-text {
  font-size: 0.8rem;
  color: #888;
}

.row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: #555;
}
.row.total {
  font-weight: bold;
  color: #000;
  font-size: 1rem;
  margin-top: 0.5rem;
}

.payment-date {
  margin-top: 1rem;
  font-size: 0.8rem;
  color: #888;
  line-height: 1.4;
}

.guide-card ul {
  list-style: none;
  padding: 0;
}
.guide-card li {
  position: relative;
  padding-left: 10px;
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 0.3rem;
}
.guide-card li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: #888;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  margin-top: 1.5rem;
}

.primary-btn {
  background: var(--primary);
  color: #004d40; /* High contrast text */
  padding: 1rem;
  border-radius: 8px;
  font-weight: bold;
  border: none;
}
.secondary-btn {
  background: white;
  border: 1px solid #ddd;
  padding: 1rem;
  border-radius: 8px;
  font-weight: bold;
  color: #555;
}
</style>
