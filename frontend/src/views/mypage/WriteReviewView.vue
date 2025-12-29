<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// Reservation data from router state
const reservation = ref({
  accommodationName: '산속 독채 숙소',
  dates: '2025-11-20 ~ 2025-11-22'
})

onMounted(() => {
  if (history.state && history.state.reservationData) {
    reservation.value = history.state.reservationData
  }
})

// Form data
const agreed = ref(false)
const rating = ref(0)
const reviewContent = ref('')
const selectedTags = ref([])

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

// Tag list from backend (mock)
const availableTags = ref([
  { id: 1, label: '환경가 좋아요', icon: '✓' },
  { id: 2, label: '밥음이 잘돼요', icon: '😊' },
  { id: 3, label: '깨끗해요', icon: '✨' },
  { id: 4, label: '화장실이 잘 되어있어요', icon: '🚿' },
  { id: 5, label: '보안시설이 잘 되어있어요', icon: '🔒' },
  { id: 6, label: '냉난방이 잘돼요', icon: '❄️' },
  { id: 7, label: '벌레 걱정 없어요', icon: '🚫' }
])

const toggleTag = (tag) => {
  const idx = selectedTags.value.findIndex(t => t.id === tag.id)
  if (idx === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(idx, 1)
  }
}

const isTagSelected = (tag) => {
  return selectedTags.value.some(t => t.id === tag.id)
}

const setRating = (star) => {
  rating.value = star
}

const handleSubmit = () => {
  if (!agreed.value) {
    openModal('리뷰 작성에 동의해주세요.', 'error')
    return
  }
  if (rating.value === 0) {
    openModal('별점을 선택해주세요.', 'error')
    return
  }
  if (!reviewContent.value.trim()) {
    openModal('리뷰 내용을 입력해주세요.', 'error')
    return
  }
  
  openModal('리뷰가 등록되었습니다!', 'success', () => router.push('/reviews'))
}
</script>

<template>
  <div class="write-review-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>리뷰 작성</h1>
    </div>

    <!-- Notice Box -->
    <div class="notice-box">
      <h3>리뷰 작성 전 확인해 주세요.</h3>
      <ul>
        <li>개인정보(실명, 얼굴사진 등)와 허위·비방 내용은 등록할 수 없습니다.</li>
        <li>리뷰와 사진은 서비스 노출 및 운영 목적에 활용될 수 있습니다.</li>
        <li>부정 리뷰(보상 목적, 방문 이력 없음 등)는 제한되며 삭제될 수 있습니다.</li>
      </ul>
      <label class="agree-label">
        <input type="checkbox" v-model="agreed" />
        <span>위 내용을 확인하고 리뷰 작성에 동의합니다.</span>
      </label>
    </div>

    <!-- Accommodation Info -->
    <div class="info-section">
      <div class="info-row">
        <span class="label">숙소 이름:</span>
        <span class="value">{{ reservation.accommodationName || reservation.title }}</span>
      </div>
      <div class="info-row">
        <span class="label">체크인/아웃:</span>
        <span class="value">{{ reservation.dates || reservation.date }}</span>
      </div>
    </div>

    <!-- Rating -->
    <div class="rating-section">
      <span class="label">별점:</span>
      <div class="stars">
        <span 
          v-for="star in 5" 
          :key="star" 
          class="star"
          :class="{ active: star <= rating }"
          @click="setRating(star)"
        >★</span>
      </div>
    </div>

    <!-- Review Content -->
    <div class="content-section">
      <label class="label">리뷰 내용:</label>
      <textarea 
        v-model="reviewContent"
        placeholder="숙소에 대한 리뷰를 작성해주세요."
      ></textarea>
    </div>

    <!-- Photo Upload -->
    <div class="photo-section">
      <span class="label">사진 첨부: 📷</span>
      <div class="photo-placeholder">
        <span>+ 사진 추가</span>
      </div>
    </div>

    <!-- Tags -->
    <div class="tags-section">
      <span class="label">리뷰 태그:</span>
      <div class="tags-grid">
        <button 
          v-for="tag in availableTags" 
          :key="tag.id"
          class="tag-btn"
          :class="{ selected: isTagSelected(tag) }"
          @click="toggleTag(tag)"
        >
          <span class="tag-icon">{{ tag.icon }}</span>
          {{ tag.label }}
        </button>
      </div>
    </div>

    <!-- Submit Button -->
    <button class="submit-btn" @click="handleSubmit">리뷰 제출</button>

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
.write-review-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
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

/* Notice Box */
.notice-box {
  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 1.2rem;
  margin-bottom: 1.5rem;
  background: #fafafa;
}

.notice-box h3 {
  font-size: 0.95rem;
  margin-bottom: 0.8rem;
}

.notice-box ul {
  list-style: disc inside;
  font-size: 0.85rem;
  color: #555;
  line-height: 1.6;
  margin-bottom: 1rem;
}

.agree-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  cursor: pointer;
}

.agree-label input {
  width: 18px;
  height: 18px;
}

/* Info Section */
.info-section {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.info-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.label {
  font-weight: 600;
  font-size: 0.9rem;
  color: #333;
}

.value {
  font-size: 0.9rem;
  color: #555;
}

/* Rating */
.rating-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stars {
  display: flex;
  gap: 4px;
}

.star {
  font-size: 1.5rem;
  color: #ddd;
  cursor: pointer;
  transition: color 0.2s;
}

.star.active {
  color: #fbbf24;
}

.star:hover {
  color: #fbbf24;
}

/* Content */
.content-section {
  margin-bottom: 1.5rem;
}

.content-section textarea {
  width: 100%;
  height: 120px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1rem;
  font-size: 0.95rem;
  resize: none;
  outline: none;
  margin-top: 0.5rem;
}

.content-section textarea:focus {
  border-color: var(--primary);
}

/* Photo */
.photo-section {
  margin-bottom: 1.5rem;
}

.photo-placeholder {
  margin-top: 0.5rem;
  width: 80px;
  height: 80px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  font-size: 0.8rem;
  cursor: pointer;
}

/* Tags */
.tags-section {
  margin-bottom: 2rem;
}

.tags-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.tag-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-btn.selected {
  background: var(--primary);
  border-color: var(--primary);
  color: #004d40;
}

.tag-icon {
  font-size: 0.9rem;
}

/* Submit */
.submit-btn {
  width: 100%;
  padding: 1rem;
  background: var(--primary);
  color: #004d40;
  font-weight: bold;
  font-size: 1rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn:hover {
  opacity: 0.9;
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
