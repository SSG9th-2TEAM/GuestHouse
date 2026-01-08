<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createReservation } from '@/api/reservationApi'
import { fetchAccommodationDetail } from '@/api/accommodation'
import { getCurrentUser } from '@/api/userClient'
import { getMyCoupons } from '@/api/couponApi'

const router = useRouter()
const route = useRoute()

const props = defineProps({
  accommodationsId: [String, Number],
  roomId: [String, Number],
  guestCount: [String, Number],
  checkin: String,
  checkout: String
})

// 숙소/객실 정보
const accommodationData = ref(null)
const selectedRoomData = ref(null)
const currentUser = ref(null)
const isDataLoading = ref(true)

// 숙소 및 객실 정보 로드
onMounted(async () => {
  const accommodationsId = parseInt(props.accommodationsId) || parseInt(route.params.id)
  const roomId = parseInt(props.roomId) || parseInt(route.query.roomId)
  
  if (accommodationsId) {
    try {
      const response = await fetchAccommodationDetail(accommodationsId)
      if (response.ok && response.data) {
        accommodationData.value = response.data
        
        // 객실 정보 찾기
        if (roomId && response.data.rooms) {
          selectedRoomData.value = response.data.rooms.find(r => 
            r.roomId === roomId || r.id === roomId
          )
        }
      }
    } catch (error) {
      console.error('숙소 정보 로드 실패:', error)
    }
  }
  
  // 현재 로그인한 사용자 정보 조회
  try {
    const userResponse = await getCurrentUser()
    if (userResponse.ok && userResponse.data) {
      currentUser.value = userResponse.data
    }
  } catch (error) {
    console.error('사용자 정보 조회 실패:', error)
  }

  // 사용 가능한 쿠폰 조회
  try {
    const couponList = await getMyCoupons('ISSUED')
    coupons.value = couponList || []
  } catch (error) {
    console.error('쿠폰 조회 실패:', error)
  }

  isDataLoading.value = false
})

// guests 텍스트에서 총 인원수 추출 (예: "게스트 2명, 아동 1명" -> 3)
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

const formatDateDisplay = (dateText) => {
  if (!dateText) return ''
  const parsed = new Date(dateText)
  if (Number.isNaN(parsed.getTime())) return ''
  const year = parsed.getFullYear()
  const month = parsed.getMonth() + 1
  const day = parsed.getDate()
  return `${year}년 ${month}월 ${day}일`
}

// Get data from query params + API
const booking = computed(() => {
  const guestCountFromProps = parseInt(props.guestCount)
  const guestCountFromQuery = parseInt(route.query.guestCount)
  const guestCount = !Number.isNaN(guestCountFromProps)
    ? guestCountFromProps
    : (!Number.isNaN(guestCountFromQuery) ? guestCountFromQuery : 1)
  const guestsText = `게스트 ${guestCount}명`

  // checkin/checkout props 우선
  let checkin = props.checkin || route.query.checkin || route.query.checkIn || null
  let checkout = props.checkout || route.query.checkout || route.query.checkOut || null

  let datesText = '날짜를 선택하세요'
  if (checkin && checkout) {
    const formattedStart = formatDateDisplay(checkin)
    const formattedEnd = formatDateDisplay(checkout)
    if (formattedStart && formattedEnd) {
      datesText = `${formattedStart} ~ ${formattedEnd}`
    }
  }
  
  const stayNights = calculateStayNights(checkin, checkout)
  
  // API에서 가져온 숙소 정보 사용
  const acc = accommodationData.value
  const room = selectedRoomData.value
  
  // 숙소 이미지 (첫 번째 이미지)
  let mainImage = 'https://picsum.photos/id/11/800/400'
  if (acc?.images && acc.images.length > 0) {
    mainImage = acc.images[0].imageUrl || mainImage
  }
  
  // 숙소 주소
  let address = ''
  if (acc) {
    address = [acc.city, acc.district, acc.township, acc.addressDetail].filter(Boolean).join(' ')
  }
  
  // 객실 가격
  const roomPrice = room?.price || room?.weekendPrice || parseInt(route.query.roomPrice) || 150000
  
  return {
    accommodationsId: parseInt(props.accommodationsId) || parseInt(route.params.id) || 2,
    roomId: parseInt(props.roomId) || parseInt(route.query.roomId) || 2,
    accommodationName: acc?.accommodationsName || route.query.accommodationName || '숙소명 없음',
    address: address || '주소 정보 없음',
    rating: acc?.rating || parseFloat(route.query.rating) || 0,
    reviewCount: acc?.reviewCount || parseInt(route.query.reviewCount) || 0,
    image: mainImage,
    roomName: room?.roomName || room?.name || route.query.roomName || '객실명 없음',
    roomDesc: room?.roomDescription || room?.description || '',
    roomCapacity: room?.maxGuests || room?.capacity || guestCount,
    dates: datesText,
    checkin: checkin,
    checkout: checkout,
    stayNights: stayNights,
    guests: guestsText,
    guestCount: guestCount,
    price: roomPrice * stayNights,
    pricePerNight: roomPrice,
    currency: 'KRW'
  }
})

const coupons = ref([])
const eligibleCoupons = computed(() => {
  const totalPrice = booking.value?.price || 0
  return coupons.value.filter((coupon) => {
    const min = coupon?.minPrice ?? 0
    return totalPrice >= min
  })
})

const selectedCoupon = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')
const isErrorModalOpen = ref(false)

// 쿠폰 할인 금액 계산
const calculateDiscount = (coupon, price) => {
  if (!coupon) return 0
  if (coupon.discountType === 'PERCENT') {
    const discount = Math.floor(price * coupon.discountValue / 100)
    return coupon.maxDiscount ? Math.min(discount, coupon.maxDiscount) : discount
  }
  return coupon.discountValue || 0
}

const couponDiscount = computed(() => {
  return calculateDiscount(selectedCoupon.value, booking.value.price)
})

const finalPrice = computed(() => {
  return booking.value.price - couponDiscount.value
})

watch(eligibleCoupons, (newList) => {
  if (
    selectedCoupon.value &&
    !newList.some((coupon) => coupon?.id === selectedCoupon.value?.id)
  ) {
    selectedCoupon.value = null
  }
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
      userCouponId: selectedCoupon.value?.id || null,
      couponDiscountAmount: couponDiscount.value || 0,
      reserverName: currentUser.value?.email || '예약자',
      reserverPhone: currentUser.value?.phone || ''
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
    if (error.message.includes('409') || error.message.includes('Conflict')) {
      errorMessage.value = '선택하신 날짜에 이미 예약이 존재합니다.<br>다른 날짜를 선택해주세요.'
      isErrorModalOpen.value = true
    } else {
      errorMessage.value = '예약 처리 중 오류가 발생했습니다.<br>다시 시도해주세요.'
      isErrorModalOpen.value = true
    }
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
          <span class="img-placeholder" v-if="!booking.image">[숙소 이미지]</span>
        </div>

        <div class="card-body">
          <!-- Property Info -->
          <div class="property-info">
            <h2>{{ booking.accommodationName }}</h2>
            <p class="address">{{ booking.address }}</p>
            <p class="rating" v-if="booking.rating">★ {{ booking.rating }} (후기 {{ booking.reviewCount }}개)</p>
          </div>

          <!-- Room Info -->
          <div class="info-row room-info-box">
            <div class="info-label">
              <span>선택 객실</span>
            </div>
            <p class="info-value room-name">{{ booking.roomName }}</p>
            <p class="info-sub" v-if="booking.roomDesc">{{ booking.roomDesc }}</p>
            <p class="info-sub">최대 {{ booking.roomCapacity }}명</p>
          </div>

          <!-- Date Section -->
          <div class="info-row">
            <div class="info-label">
              <span>날짜</span>
            </div>
            <p class="info-value">{{ booking.dates }} ({{ booking.stayNights }}박)</p>
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
              <option
                v-for="coupon in eligibleCoupons"
                :key="coupon.id"
                :value="coupon"
              >
                {{ coupon.name }} ({{ coupon.discountType === 'PERCENT' ? coupon.discountValue + '%' : coupon.discountValue.toLocaleString() + '원' }} 할인)
              </option>
            </select>
          </div>

          <hr class="divider" />

          <!-- Price Section -->
          <div class="price-section">
            <div class="price-row">
              <span>₩{{ booking.pricePerNight.toLocaleString() }} x {{ booking.stayNights }}박</span>
              <span>₩{{ booking.price.toLocaleString() }}</span>
            </div>
            <div class="price-row" v-if="selectedCoupon">
              <span>쿠폰 할인 ({{ selectedCoupon.name }})</span>
              <span class="discount-text">-₩{{ couponDiscount.toLocaleString() }}</span>
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
            <p v-if="booking.checkin">
              체크인 날짜인 {{ formatDateDisplay(booking.checkin) }} 전에 취소하면 부분 환불을 받으실 수 있습니다.
            </p>
            <p v-else>취소 시 환불 정책이 적용됩니다.</p>
            <router-link to="/policy?tab=refund" class="policy-link">환불 정책 전문</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Action -->
    <div class="bottom-action">
      <button 
        class="pay-btn" 
        :disabled="isLoading"
        @click="handlePayment"
      >
        {{ isLoading ? '처리 중...' : '결제하기' }}
      </button>
    </div>
  </div>

  <!-- Error Modal -->
  <div v-if="isErrorModalOpen" class="modal-overlay" @click.self="isErrorModalOpen = false">
    <div class="modal-content error-modal">
      <div class="modal-icon error">⚠️</div>
      <h3>예약 실패</h3>
      <p class="modal-desc" v-html="errorMessage"></p>
      <button class="close-modal-btn" @click="isErrorModalOpen = false">확인</button>
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

.address {
  font-size: 0.85rem;
  color: var(--text-sub);
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

.room-info-box {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

.room-name {
  font-weight: 600;
  font-size: 1rem;
}

.info-sub {
  font-size: 0.85rem;
  color: var(--text-sub);
  margin-top: 0.25rem;
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
  z-index: 100;
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

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-content {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  text-align: center;
  padding: 2rem;
  width: 90%;
  max-width: 360px;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.modal-content h3 {
  font-size: 1.2rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.modal-desc {
  color: #666;
  font-size: 0.95rem;
  line-height: 1.5;
  margin-bottom: 1.5rem;
}

.close-modal-btn {
  background: var(--primary);
  color: #004d40;
  border: none;
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
}

.error-message {
  display: none;
}
</style>
