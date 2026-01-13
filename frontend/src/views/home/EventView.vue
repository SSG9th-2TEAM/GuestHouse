<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDownloadableCoupons, issueCoupon, getMyCouponIds, getMyCoupons } from '@/api/couponApi'
import { getAccessToken, getUserId } from '@/api/authClient'

const router = useRouter()
const couponEvents = ref([])
const couponLoading = ref(false)
const couponError = ref(null)
const flashCouponCode = 'DAILY_FLASH50'
const nextResetCountdown = ref('00:00:00')
const countdownInterval = ref(null)
const claimedCoupons = ref(new Set())
const issuingMap = ref({})
const isModalOpen = ref(false)
const modalTitle = ref('')
const modalMessage = ref('')

const normalizeId = (value) => (value === null || value === undefined ? null : String(value))
const claimedStorageKey = () => {
  const userId = getUserId()
  return userId ? `claimedCoupons:${userId}` : null
}

const getNextResetTs = () => {
  const now = new Date()
  const nextMidnight = new Date(now)
  nextMidnight.setHours(24, 0, 0, 0)
  return nextMidnight.getTime()
}

const loadLocalClaimedCoupons = () => {
  const key = claimedStorageKey()
  if (!key) return new Set()
  const raw = sessionStorage.getItem(key)
  if (!raw) return new Set()
  try {
    const data = JSON.parse(raw)
    if (data?.expiresAt && Date.now() > data.expiresAt) {
      sessionStorage.removeItem(key)
      return new Set()
    }
    const ids = Array.isArray(data?.ids) ? data.ids : []
    const normalized = ids
      .map((id) => normalizeId(id))
      .filter(Boolean)
    return new Set(normalized)
  } catch (error) {
    console.warn('⚠️ [EventView] 로컬 쿠폰 데이터 파싱 실패:', error)
    return new Set()
  }
}

const saveLocalClaimedCoupons = (idsSet) => {
  const key = claimedStorageKey()
  if (!key) return
  const payload = {
    expiresAt: getNextResetTs(),
    ids: Array.from(idsSet)
  }
  sessionStorage.setItem(key, JSON.stringify(payload))
}

const fetchCoupons = async () => {
  couponLoading.value = true
  couponError.value = null
  try {
    const response = await getDownloadableCoupons(0)
    const list = Array.isArray(response) ? response : []
    const prioritized = list.slice().sort((a, b) => {
      const aPriority = a.code === flashCouponCode ? -1 : 0
      const bPriority = b.code === flashCouponCode ? -1 : 0
      if (aPriority !== bPriority) {
        return aPriority - bPriority
      }
      return 0
    })
    couponEvents.value = prioritized
  } catch (error) {
    console.error('쿠폰 데이터 조회 실패:', error)
    couponEvents.value = []
    couponError.value = error?.message || '쿠폰을 불러오지 못했습니다.'
  } finally {
    couponLoading.value = false
  }
}

const loadClaimedCoupons = async () => {
  if (!getAccessToken()) {
    claimedCoupons.value = new Set()
    return
  }
  try {
    const result = await getMyCoupons('ALL')
    console.log('🔍 [EventView] 내 쿠폰 목록 조회:', result)
    
    if (!Array.isArray(result)) {
      console.warn('⚠️ [EventView] 쿠폰 목록이 배열이 아닙니다:', result)
      return
    }
    
    const ids = new Set()
    result.forEach((userCoupon) => {
      // UserCouponResponseDto는 flatten된 구조 (couponId 직접 접근)
      const couponId = userCoupon.couponId || userCoupon.coupon?.couponId
      if (couponId) {
        const normalized = normalizeId(couponId)
        if (normalized) {
          ids.add(normalized)
          console.log(`✅ [EventView] 쿠폰 ${couponId} → normalized: ${normalized}`)
        }
      }
    })
    
    console.log('📋 [EventView] 발급받은 쿠폰 IDs:', Array.from(ids))
    const merged = new Set(claimedCoupons.value)
    ids.forEach((id) => merged.add(id))
    claimedCoupons.value = merged
    saveLocalClaimedCoupons(merged)
  } catch (error) {
    console.error('❌ [EventView] 쿠폰 목록 조회 실패:', error)
  }
}

const clearCountdown = () => {
  if (countdownInterval.value) {
    clearInterval(countdownInterval.value)
    countdownInterval.value = null
  }
}

const startCountdown = () => {
  clearCountdown()
  countdownInterval.value = setInterval(updateCountdown, 1000)
}

const updateCountdown = async () => {
  const now = Date.now()
  const diffMs = getNextResetTs() - now
  if (diffMs <= 0) {
    nextResetCountdown.value = '00:00:00'
    clearCountdown()
    claimedCoupons.value = loadLocalClaimedCoupons()
    await fetchCoupons()
    await loadClaimedCoupons()
    startCountdown()
    return
  }
  const hours = Math.floor(diffMs / (1000 * 60 * 60))
  const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diffMs % (1000 * 60)) / 1000)
  nextResetCountdown.value = [
    String(hours).padStart(2, '0'),
    String(minutes).padStart(2, '0'),
    String(seconds).padStart(2, '0')
  ].join(':')
}

const isIssuing = (couponId) => {
  const key = normalizeId(couponId)
  return Boolean(key && issuingMap.value[key])
}

const isClaimed = (couponId) => {
  const key = normalizeId(couponId)
  return key ? claimedCoupons.value.has(key) : false
}

const markClaimedCoupon = (couponId) => {
  const key = normalizeId(couponId)
  if (!key) return
  const next = new Set(claimedCoupons.value)
  next.add(key)
  claimedCoupons.value = next
  saveLocalClaimedCoupons(next)
}

const openModal = (title, message) => {
  modalTitle.value = title
  modalMessage.value = message
  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
}

const handleClaimCoupon = async (coupon) => {
  if (!getAccessToken()) {
    alert('로그인이 필요한 서비스입니다.')
    router.push({ path: '/login', query: { redirect: '/events' } })
    return
  }
  if (!coupon?.couponId || isIssuing(coupon.couponId) || isClaimed(coupon.couponId)) {
    return
  }
  const key = normalizeId(coupon.couponId)
  issuingMap.value = { ...issuingMap.value, [key]: true }
  try {
    await issueCoupon(coupon.couponId)
    // 발급 성공 시 즉시 버튼 상태 업데이트 (새로고침 없이)
    markClaimedCoupon(coupon.couponId)
    // 백그라운드에서 쿠폰 목록 동기화
    loadClaimedCoupons()
    openModal('쿠폰 발급 완료', '쿠폰이 발급되었습니다. 쿠폰함에서 확인하세요.')
  } catch (error) {
    const message = error?.message || '쿠폰 발급에 실패했습니다.'
    if (message.includes('이미 발급')) {
      markClaimedCoupon(coupon.couponId)
    }
    openModal('쿠폰 발급 안내', message)
  } finally {
    const nextState = { ...issuingMap.value }
    delete nextState[key]
    issuingMap.value = nextState
  }
}

const formatDiscount = (coupon) => {
  if (coupon.discountType === 'PERCENT') {
    return `${coupon.discountValue ?? 0}% 할인`
  }
  return `${(coupon.discountValue ?? 0).toLocaleString()}원 할인`
}

const formatMinSpend = (minPrice) => {
  if (!minPrice || Number(minPrice) === 0) {
    return '제한 없음'
  }
  return `${Number(minPrice).toLocaleString()}원 이상 사용 시`
}

const formatPeriod = (start, end) => {
  if (!start && !end) return '상시 진행'
  const toDate = (value) => {
    const d = new Date(value)
    return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
  }
  if (start && end) {
    return `${toDate(start)} ~ ${toDate(end)}`
  }
  if (end) return `${toDate(end)}까지`
  return '상시 진행'
}

onMounted(async () => {
  claimedCoupons.value = loadLocalClaimedCoupons()
  await fetchCoupons()
  await loadClaimedCoupons()
  await updateCountdown()
  startCountdown()
})

onUnmounted(() => {
  clearCountdown()
})
</script>

<template>
  <div class="event-page container">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">←</button>
      <div>
        <h1>이벤트 & 프로모션</h1>
        <p>지금이곳에서 진행 중인 다양한 혜택을 한눈에 확인해보세요.</p>
      </div>
    </div>

    <section class="coupon-section">
      <div class="section-header">
        <div>
          <h2>지금 다운로드 가능한 쿠폰</h2>
          <p>사이트 공용 쿠폰은 모든 숙소에서 사용할 수 있어요.</p>
        </div>
        <button class="link-btn" @click="router.push('/coupons')">
          내 쿠폰함으로 이동
        </button>
      </div>

      <div v-if="couponLoading" class="coupon-state">
        쿠폰을 불러오는 중입니다...
      </div>
      <div v-else-if="couponError" class="coupon-state error">
        <p>{{ couponError }}</p>
        <button @click="fetchCoupons">다시 시도</button>
      </div>
      <div v-else-if="couponEvents.length === 0" class="coupon-state empty">
        현재 다운로드 가능한 쿠폰이 없습니다. 잠시 후 다시 확인해주세요.
      </div>
      <div v-else class="coupon-grid">
        <article
          v-for="coupon in couponEvents"
          :key="coupon.couponId"
          class="coupon-card"
          :class="{ 'coupon-card--flash': coupon.code === flashCouponCode }"
        >
          <div class="coupon-body">
          <div class="coupon-chip-group">
            <span class="coupon-chip">다운로드형 쿠폰</span>
            <span
              v-if="coupon.code === flashCouponCode"
              class="coupon-chip coupon-chip--flash"
            >
              선착순 50장
            </span>
          </div>
          <h3>{{ coupon.name }}</h3>
          <p>{{ coupon.description || '상세 설명이 제공되지 않은 쿠폰입니다.' }}</p>
          <ul class="coupon-meta">
            <li>{{ formatDiscount(coupon) }}</li>
            <li>{{ formatMinSpend(coupon.minPrice) }}</li>
            <li>{{ formatPeriod(coupon.validFrom, coupon.validTo) }}</li>
          </ul>
        </div>
        <div class="coupon-card__footer">
          <button
            class="coupon-cta"
            :disabled="isIssuing(coupon.couponId) || isClaimed(coupon.couponId)"
            @click="handleClaimCoupon(coupon)"
          >
            <span v-if="isClaimed(coupon.couponId)">발급 완료</span>
            <span v-else-if="isIssuing(coupon.couponId)">발급 중...</span>
            <span v-else>즉시 발급</span>
          </button>
          <span
            v-if="coupon.code === flashCouponCode"
            class="flash-countdown"
          >
            리셋 {{ nextResetCountdown }}
          </span>
        </div>
      </article>
      </div>
    </section>

    <div v-if="isModalOpen" class="event-modal-overlay" @click.self="closeModal">
      <div class="event-modal-content" role="dialog" aria-modal="true">
        <h3 class="event-modal-title">{{ modalTitle }}</h3>
        <p class="event-modal-message">{{ modalMessage }}</p>
        <button class="event-modal-btn" type="button" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 4px;
  font-size: 1.75rem;
  color: #111;
}

.page-header p {
  margin: 0;
  color: #667085;
}

.back-btn {
  border: none;
  background: #f5f5f5;
  border-radius: 999px;
  width: 44px;
  height: 44px;
  font-size: 1.25rem;
  cursor: pointer;
  transition: background 0.2s ease;
}

.back-btn:hover {
  background: #e5e7eb;
}

.coupon-section {
  margin-top: 16px;
  padding: 32px 28px;
  background: #f8fafc;
  border-radius: 24px;
  border: 1px solid #e2e8f0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.section-header h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
  color: #0f172a;
}

.section-header p {
  margin: 0;
  color: #475467;
}

.link-btn {
  border: none;
  background: transparent;
  color: #0ea5e9;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
}

.coupon-state {
  text-align: center;
  padding: 40px 16px;
  color: #475467;
}

.coupon-state.error {
  color: #dc2626;
}

.coupon-state button {
  margin-top: 12px;
  padding: 8px 16px;
  border-radius: 999px;
  border: none;
  background: #0ea5e9;
  color: white;
  cursor: pointer;
}

.coupon-grid {
  display: grid;
  gap: 20px;
}

.coupon-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
}

.coupon-body h3 {
  margin: 8px 0;
  font-size: 1.25rem;
  color: #0f172a;
}

.coupon-body p {
  margin: 0;
  color: #475467;
}

.coupon-chip-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.coupon-chip {
  display: inline-flex;
  font-size: 0.85rem;
  background: #e0f2fe;
  color: #0369a1;
  padding: 4px 12px;
  border-radius: 999px;
  font-weight: 600;
}

.coupon-chip--flash {
  background: #fef3c7;
  color: #b45309;
}

.coupon-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.flash-countdown {
  font-weight: 600;
  color: #dc2626;
  margin-left: auto;
}

.coupon-meta {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  color: #475467;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-cta {
  align-self: flex-start;
  padding: 10px 18px;
  border-radius: 12px;
  border: none;
  background: #066cc0;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.coupon-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 16px rgba(6, 108, 192, 0.2);
}

.coupon-cta:disabled {
  background: #cbd5e1;
  color: #64748b;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.event-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.event-modal-content {
  background: #fff;
  border-radius: 18px;
  padding: 24px 28px;
  max-width: 360px;
  width: 100%;
  text-align: center;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.2);
}

.event-modal-title {
  margin: 0 0 8px;
  font-size: 1.15rem;
  color: #0f172a;
}

.event-modal-message {
  margin: 0 0 20px;
  color: #475467;
  white-space: pre-wrap;
}

.event-modal-btn {
  border: none;
  background: var(--brand-primary-strong, #6DC3BB);
  color: var(--brand-on-primary, #0f172a);
  padding: 10px 20px;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
}

.event-modal-btn:hover {
  background: var(--brand-primary, #BFE7DF);
}

@media (max-width: 640px) {
  .section-header {
    flex-direction: column;
  }

  .coupon-card {
    padding: 20px;
  }

  .coupon-meta {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .coupon-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
