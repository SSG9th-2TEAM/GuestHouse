<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createReservation } from '@/api/reservationApi'

const router = useRouter()
const route = useRoute()

// guests 텍스트에서 총 인원수 추출 (예: "성인 2명, 아동 1명" -> 3)
const parseGuestCount = (guestsText) => {
  if (!guestsText) return 1
  const matches = guestsText.match(/(\d+)/g)
  if (matches) {
    return matches.reduce((sum, num) => sum + parseInt(num), 0)
  }
  return 1
}

// 숙박 일수 계산
const calculateStayNights = (checkin, checkout) => {
  if (!checkin || !checkout) return 1
  const checkinDate = new Date(checkin)
  const checkoutDate = new Date(checkout)
  const diffTime = checkoutDate.getTime() - checkinDate.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays > 0 ? diffDays : 1
}

// dates 텍스트에서 체크인/체크아웃 파싱 (예: "2025년 12월 24일 ~ 2025년 12월 26일")
const parseDatesText = (datesText) => {
  if (!datesText) return { checkin: null, checkout: null }
  
  // 패턴: "YYYY년 MM월 DD일 ~ YYYY년 MM월 DD일" 또는 "MM월 DD일 ~ MM월 DD일"
  const regex = /(\d{4})년?\s*(\d{1,2})월\s*(\d{1,2})일/g
  const matches = [...datesText.matchAll(regex)]
  
  if (matches.length >= 2) {
    const year1 = matches[0][1]
    const month1 = matches[0][2].padStart(2, '0')
    const day1 = matches[0][3].padStart(2, '0')
    
    const year2 = matches[1][1]
    const month2 = matches[1][2].padStart(2, '0')
    const day2 = matches[1][3].padStart(2, '0')
    
    return {
      checkin: `${year1}-${month1}-${day1}`,
      checkout: `${year2}-${month2}-${day2}`
    }
  }
  
  // 년도 없는 패턴: "12월 24일 ~ 12월 26일"
  const regexNoYear = /(\d{1,2})월\s*(\d{1,2})일/g
  const matchesNoYear = [...datesText.matchAll(regexNoYear)]
  
  if (matchesNoYear.length >= 2) {
    const currentYear = new Date().getFullYear()
    const month1 = matchesNoYear[0][1].padStart(2, '0')
    const day1 = matchesNoYear[0][2].padStart(2, '0')
    const month2 = matchesNoYear[1][1].padStart(2, '0')
    const day2 = matchesNoYear[1][2].padStart(2, '0')
    
    return {
      checkin: `${currentYear}-${month1}-${day1}`,
      checkout: `${currentYear}-${month2}-${day2}`
    }
  }
  
  return { checkin: null, checkout: null }
}

// Get data from query params
const booking = computed(() => {
  const guestsText = route.query.guests || '성인 1명'
  const guestCountFromQuery = parseInt(route.query.guestCount)
  const guestCount = !isNaN(guestCountFromQuery) ? guestCountFromQuery : parseGuestCount(guestsText)
  
  // checkin/checkout 쿼리 파라미터가 없으면 dates 텍스트에서 파싱
  const datesText = route.query.dates || '날짜를 선택하세요'
  let checkin = route.query.checkin || route.query.checkIn || null
  let checkout = route.query.checkout || route.query.checkOut || null
  
  if (!checkin || !checkout) {
    const parsedDates = parseDatesText(datesText)
    checkin = checkin || parsedDates.checkin
    checkout = checkout || parsedDates.checkout
  }
  
  const stayNights = calculateStayNights(checkin, checkout)
  
  return {
    accommodationsId: parseInt(route.params.id) || 2,
    roomId: parseInt(route.query.roomId) || 2, // Defaulting to 2 if missing, or handle as null
    hotelName: route.query.hotelName || '그랜드 호텔 서울',
    rating: parseFloat(route.query.rating) || 4.8,
    reviewCount: parseInt(route.query.reviewCount) || 219,
    image: route.query.image || 'https://picsum.photos/id/11/800/400',
    roomName: route.query.roomName || '스탠다드 룸',
    dates: datesText,
    checkin: checkin,
    checkout: checkout,
    stayNights: stayNights,
    guests: guestsText,
    guestCount: guestCount,
    price: parseInt(route.query.roomPrice) || 150000,
    currency: 'KRW',
    alertMessage: '본 숙소 앞으로 기차나 기숙이 속으로 보는 예약이 가능 차 있습니다'
  }
})

const coupons = [
  { id: 1, name: '신규 가입 환영 쿠폰', discount: 10000 },
  { id: 2, name: '주말 깜짝 할인', discount: 5000 }
]

const selectedCoupon = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')

const finalPrice = computed(() => {
  const discount = selectedCoupon.value ? selectedCoupon.value.discount : 0
  return booking.value.price - discount
})

const goBack = () => router.back()

// 예약 생성 및 결제 페이지로 이동
const handlePayment = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    // 체크인/체크아웃 날짜 파싱 (없으면 기본값 사용)
    const now = new Date()
    const checkinDate = booking.value.checkin 
      ? new Date(booking.value.checkin) 
      : new Date(now.getTime() + 24 * 60 * 60 * 1000) // 내일
    const checkoutDate = booking.value.checkout 
      ? new Date(booking.value.checkout) 
      : new Date(now.getTime() + 2 * 24 * 60 * 60 * 1000) // 모레

    console.log('예약 생성 데이터:', {
      checkin: booking.value.checkin,
      checkout: booking.value.checkout,
      guestCount: booking.value.guestCount,
      stayNights: booking.value.stayNights,
      guests: booking.value.guests
    })

    const reservationData = {
      accommodationsId: booking.value.accommodationsId,
      roomId: booking.value.roomId,
      // userId는 백엔드에서 JWT 토큰으로부터 추출
      checkin: checkinDate.toISOString(),
      checkout: checkoutDate.toISOString(),
      guestCount: booking.value.guestCount,
      totalAmount: booking.value.price,
      couponId: selectedCoupon.value?.id || null,
      couponDiscountAmount: selectedCoupon.value?.discount || 0,
      reserverName: '홍길동', // TODO: 실제 사용자 정보로 대체
      reserverPhone: '010-1234-5678' // TODO: 실제 사용자 정보로 대체
    }
    
    console.log('Final Reservation Data Payload:', JSON.stringify(reservationData, null, 2))

    const response = await createReservation(reservationData)

    // 예약 성공 시 결제 페이지로 이동
    router.push({
      name: 'payment',
      params: { reservationId: response.reservationId }
    })
  } catch (error) {
    console.error('예약 생성 실패:', error)
    errorMessage.value = '예약 처리 중 오류가 발생했습니다. 다시 시도해주세요.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="booking-page">
    <!-- Header -->
    <header class="header">
      <button class="icon-btn" @click="goBack">←</button>
    </header>

    <div class="container content">
      <h1 class="page-title">검토 후 계속 진행</h1>

      <!-- Booking Card -->
      <div class="booking-card">
        <!-- Hero Image -->
        <div class="card-image" :style="{ backgroundImage: `url(${booking.image})` }">
          <span class="img-placeholder" v-if="!booking.image">[호텔 이미지]</span>
        </div>

        <div class="card-body">
          <!-- Property Info -->
          <div class="property-info">
            <h2>{{ booking.hotelName }}</h2>
            <p class="rating">★ {{ booking.rating }} (후기 {{ booking.reviewCount }}개) · 게스트 선호</p>
          </div>

          <!-- Date Section -->
          <div class="info-row">
            <div class="info-label">
              <span>날짜</span>
            </div>
            <p class="info-value">{{ booking.dates }}</p>
          </div>

          <!-- Alert/Banner -->
          <div class="alert-box">
             <span class="alert-text">{{ booking.alertMessage }}</span>
          </div>

          <!-- Guest Section -->
          <div class="info-row">
            <div class="info-label">
              <span>게스트</span>
            </div>
            <p class="info-value">{{ booking.guests }}</p>
          </div>

          <!-- Coupon Section -->
          <div class="info-row">
            <div class="info-label">
              <span>쿠폰</span>
            </div>
            <select v-model="selectedCoupon" class="coupon-select">
              <option :value="null">쿠폰 선택 안함</option>
              <option v-for="coupon in coupons" :key="coupon.id" :value="coupon">
                {{ coupon.name }} (-{{ coupon.discount.toLocaleString() }}원)
              </option>
            </select>
          </div>

          <hr class="divider" />

          <!-- Price Section -->
          <div class="price-section">
            <div class="price-row">
              <span>객실 요금</span>
              <span>₩{{ booking.price.toLocaleString() }}</span>
            </div>
            <div class="price-row" v-if="selectedCoupon">
              <span>쿠폰 할인</span>
              <span class="discount-text">-₩{{ selectedCoupon.discount.toLocaleString() }}</span>
            </div>
            <div class="total-row">
              <span>총 합계</span>
              <div class="total-price">
                ₩{{ finalPrice.toLocaleString() }} <span class="currency">{{ booking.currency }}</span>
              </div>
            </div>
          </div>

          <hr class="divider" />

          <!-- Policy Section -->
          <div class="policy-section">
            <p>체크인 날짜인 12월 12일 전에 취소하면 부분 환불을 받으실 수 있습니다.</p>
            <a href="#" class="policy-link">환불 정책 전문</a>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Action -->
    <div class="bottom-action">
      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
      <button 
        class="pay-btn" 
        :disabled="isLoading"
        @click="handlePayment"
      >
        {{ isLoading ? '처리 중...' : '결제하기' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.booking-page {
  background-color: white;
  min-height: 100vh;
  padding-bottom: 100px;
}

.header {
  display: flex;
  justify-content: space-between;
  padding: 1rem;
  background: white;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.content {
  padding: 0 1.5rem;
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
  font-weight: normal;
}

/* Card Styles */
.booking-card {
  border: 1px solid #ddd;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08); /* Soft shadow like the frame */
  background: white;
}

.card-image {
  height: 200px;
  background-color: #e2e8f0;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.card-body {
  padding: 1.5rem;
}

.property-info {
  margin-bottom: 1.5rem;
}

.property-info h2 {
  font-size: 1.1rem;
  margin-bottom: 0.3rem;
}

.rating {
  font-size: 0.9rem;
  color: var(--text-main);
}

/* Info Rows */
.info-row {
  margin-bottom: 1.5rem;
}

.info-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-weight: bold;
  font-size: 0.95rem;
}

.edit-btn {
  background: transparent;
  border: 1px solid var(--text-main); /* Or #333 */
  border-radius: 20px; /* Capsule shape */
  padding: 4px 12px;
  font-size: 0.8rem;
  font-weight: bold;
}

.info-value {
  font-size: 0.95rem;
}

/* Alert */
.alert-box {
  background-color: transparent; /* No bg in screenshot explicitly, just icon and text */
  margin-bottom: 1.5rem;
  display: flex;
  gap: 8px;
  font-size: 0.9rem;
  color: #e11d48; /* Reddish text for alert */
  line-height: 1.4;
}
.alert-text {
  color: #0d2c26 ;
}

.divider {
  border: 0;
  border-top: 1px solid #eee;
  margin: 1.5rem 0;
}

/* Price */
.price-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.price-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
  color: var(--text-sub);
}

.discount-text {
  color: #e11d48;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
  font-weight: bold;
}

.total-price {
  font-size: 1.4rem;
  font-weight: 800;
}

.currency {
  font-size: 1rem;
  font-weight: normal;
}

.coupon-select {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  font-size: 0.95rem;
  outline: none;
  cursor: pointer;
}
.coupon-select:focus {
  border-color: var(--primary);
}

/* Policy */
.policy-section {
  font-size: 0.85rem;
  color: var(--text-sub);
  line-height: 1.5;
}

.policy-link {
  color: black;
  text-decoration: underline;
  font-weight: bold;
  display: block;
  margin-top: 0.5rem;
}

/* Bottom Action */
.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1rem;
  background: white;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: center;
  box-sizing: border-box;
}

.pay-btn {
  width: calc(100% - 3rem); /* Account for parent padding */
  max-width: 600px;
  background-color: var(--primary);
  color: #004d40;
  padding: 1rem;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: bold;
  box-sizing: border-box;
  cursor: pointer;
  transition: opacity 0.2s;
}

.pay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  width: 100%;
  max-width: 600px;
  color: #e11d48;
  font-size: 0.9rem;
  text-align: center;
  margin-bottom: 0.5rem;
}
</style>
