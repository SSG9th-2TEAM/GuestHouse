<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHostReviews, createHostReviewReply, reportHostReview } from '@/api/hostReview'
import { formatDate } from '@/utils/formatters'

const reviews = ref([])
const isLoading = ref(false)
const loadError = ref('')
const router = useRouter()

const averageRating = computed(() => {
  if (!reviews.value.length) return '0.0'
  const sum = reviews.value.reduce((acc, curr) => acc + curr.rating, 0)
  return (sum / reviews.value.length).toFixed(1)
})

const replyText = ref({})

const toggleReplyForm = (reviewId) => {
  const review = reviews.value.find(r => r.id === reviewId)
  if (review) {
    review.showReplyForm = !review.showReplyForm
    if (review.showReplyForm && !replyText.value[reviewId]) {
      replyText.value[reviewId] = review.reply || ''
    }
  }
}

const submitReply = async (reviewId) => {
  const review = reviews.value.find(r => r.id === reviewId)
  const content = replyText.value[reviewId]?.trim()
  if (review && content) {
    const response = await createHostReviewReply(reviewId, { content })
    if (response.ok) {
      review.reply = content
      review.showReplyForm = false
      alert('답변이 등록되었습니다.')
      return
    }
    alert('답변 등록에 실패했습니다.')
  }
}

const cancelReply = (reviewId) => {
  const review = reviews.value.find(r => r.id === reviewId)
  if (review) {
    review.showReplyForm = false
    replyText.value[reviewId] = review.reply || ''
  }
}

const normalizeReview = (item) => {
  const userName = item.userName ?? item.reviewerName ?? item.name ?? ''
  return {
    id: item.reviewId ?? item.id,
    userName,
    userInitial: userName ? userName.slice(0, 1) : 'U',
    accommodationName: item.accommodationName ?? item.property ?? '',
    rating: item.rating ?? 0,
    date: formatDate(item.createdAt ?? item.date, true),
    content: item.content ?? item.reviewContent ?? '',
    reply: item.reply ?? item.replyContent ?? '',
    showReplyForm: false,
    reported: Boolean(item.reported)
  }
}

const loadReviews = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchHostReviews({ periodDays: 30 })
  if (response.ok) {
    const payload = response.data
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    reviews.value = list.map(normalizeReview)
  } else {
    loadError.value = '리뷰를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const reportReview = async (reviewId) => {
  const review = reviews.value.find(r => r.id === reviewId)
  if (!review || review.reported) return
  const reason = prompt('신고 사유를 입력해주세요.')
  if (!reason) return
  const response = await reportHostReview(reviewId, { reason })
  if (response.ok) {
    review.reported = true
    alert('리뷰가 신고되었습니다.')
    return
  }
  alert('리뷰 신고에 실패했습니다.')
}

onMounted(loadReviews)
</script>

<template>
  <div class="review-view">
    <div class="view-header">
      <h2>리뷰 관리</h2>
      <p class="subtitle">평균 평점 {{ averageRating }} · 총 {{ reviews.length }}개의 리뷰</p>
    </div>

    <div class="review-list">
      <div v-if="isLoading" class="review-skeleton">
        <div v-for="i in 3" :key="i" class="skeleton-card" />
      </div>
      <div v-else-if="loadError" class="empty-box">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" @click="loadReviews">다시 시도</button>
      </div>
      <div v-else-if="!reviews.length" class="empty-box">
        <p>아직 리뷰가 없습니다.</p>
        <button class="ghost-btn" @click="router.push('/host/booking')">예약 확인하기</button>
      </div>
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <!-- User Header -->
        <div class="card-header">
          <div class="user-profile">
            <div class="avatar">{{ review.userInitial }}</div>
            <div class="user-info">
              <span class="user-name">{{ review.userName }}</span>
              <span class="accommodation-name">{{ review.accommodationName }}</span>
            </div>
          </div>

          <div class="meta-info">
            <div class="rating">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= review.rating }">★</span>
            </div>
            <span class="date">{{ review.date }}</span>
          </div>
        </div>

        <!-- Review Content -->
        <div class="review-content">
          <p>{{ review.content }}</p>
        </div>

        <!-- Host Reply Section -->
        <div class="reply-section">
          <!-- Existing Reply -->
          <div v-if="review.reply && !review.showReplyForm" class="existing-reply">
            <div class="reply-header">
              <span class="host-label">호스트의 답글</span>
              <button class="edit-reply-btn" @click="toggleReplyForm(review.id)">수정</button>
            </div>
            <p class="reply-text">{{ review.reply }}</p>
          </div>

          <!-- Reply Form -->
          <div v-else-if="review.showReplyForm" class="reply-form">
            <textarea
                v-model="replyText[review.id]"
                placeholder="답변을 작성하세요..."
                rows="3"
            ></textarea>
            <div class="form-actions">
              <button class="btn-cancel" @click="cancelReply(review.id)">취소</button>
              <button class="btn-submit host-btn--primary" @click="submitReply(review.id)">답변 등록</button>
            </div>
          </div>

          <!-- Reply Button (If no reply and form closed) -->
          <button
            v-else
            class="btn-reply-toggle"
            @click="toggleReplyForm(review.id)"
          >
            답글 달기
          </button>
        </div>

        <!-- Report Button -->
        <div class="card-footer">
          <button class="btn-report" :disabled="review.reported" @click="reportReview(review.id)">
            <span class="icon">🚩</span> {{ review.reported ? '신고 완료' : '신고하기' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.review-view {
  padding-bottom: 2rem;
}

/* ✅ 대시보드 톤 헤더 */
.view-header {
  margin-bottom: 1.25rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--brand-accent);
  margin: 0.15rem 0 0.2rem;
  letter-spacing: -0.01em;
}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0;
}

/* Review Card (대시보드 카드 톤) */
.review-card {
  background: white;
  border-radius: 16px;
  padding: 1.25rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  margin-bottom: 1rem;
  border: 1px solid var(--brand-border);
}

.empty-box {
  margin-top: 1rem;
  color: #6b7280;
  font-weight: 700;
  text-align: center;
  display: grid;
  gap: 0.5rem;
  justify-items: center;
  padding: 0.5rem 0;
}

.ghost-btn {
  border: 1px solid var(--brand-border);
  background: white;
  color: var(--brand-accent);
  border-radius: 10px;
  padding: 0.55rem 0.9rem;
  font-weight: 800;
  min-height: 44px;
  cursor: pointer;
}

.review-skeleton {
  display: grid;
  gap: 0.75rem;
}

.skeleton-card {
  height: 140px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-card {
    animation: none !important;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
}

.user-profile {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  min-width: 0;
}

.avatar {
  width: 48px;
  height: 48px;
  background: var(--brand-primary);
  color: var(--brand-accent);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 1.05rem;
  flex: 0 0 auto;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.user-name {
  font-weight: 900;
  color: #0f172a;
  font-size: 1rem;
}

.accommodation-name {
  font-size: 0.88rem;
  color: #6b7280;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-info {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  flex: 0 0 auto;
}

.rating {
  color: #e5e7eb;
  letter-spacing: -2px;
  font-size: 0.95rem;
}

.star.filled {
  color: #FFB300;
}

.date {
  font-size: 0.85rem;
  color: #6b7280;
  font-weight: 700;
}

/* 본문 */
.review-content {
  margin-bottom: 1rem;
  font-size: 0.95rem;
  color: #0f172a;
  line-height: 1.65;
  font-weight: 600;
}

.review-content p {
  margin: 0;
}

/* Reply Section */
.reply-section {
  margin-top: 0.75rem;
}

.reply-form textarea {
  width: 100%;
  padding: 0.9rem 1rem;
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  font-size: 0.95rem;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
  font-weight: 600;
}

.reply-form textarea:focus {
  outline: none;
  border-color: var(--brand-primary-strong);
  box-shadow: 0 0 0 3px var(--brand-primary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.btn-cancel,
.btn-submit {
  height: 44px;
  padding: 0 1rem;
  border-radius: 12px;
  font-size: 0.92rem;
  font-weight: 900;
  cursor: pointer;
}

.btn-cancel {
  background: white;
  border: 1px solid var(--brand-border);
  color: #475569;
}

.btn-submit {
  border: none;
}

.btn-reply-toggle {
  background: none;
  border: none;
  color: var(--brand-accent);
  font-weight: 900;
  cursor: pointer;
  padding: 0;
  font-size: 0.92rem;
}

.existing-reply {
  background: #f8fafc;
  padding: 0.95rem 1rem;
  border-radius: 12px;
  border: 1px solid #eef2f7;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.host-label {
  font-size: 0.88rem;
  font-weight: 900;
  color: #0f172a;
}

.edit-reply-btn {
  background: none;
  border: none;
  color: #6b7280;
  font-size: 0.85rem;
  cursor: pointer;
  text-decoration: underline;
  font-weight: 800;
}

.reply-text {
  font-size: 0.92rem;
  color: #334155;
  margin: 0;
  line-height: 1.6;
  font-weight: 600;
}

/* Footer */
.card-footer {
  margin-top: 0.9rem;
  display: flex;
  justify-content: flex-end;
}

.btn-report {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 0.88rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  font-weight: 900;
}

.btn-report:disabled {
  color: #9ca3af;
  cursor: default;
}

.icon {
  font-size: 1rem;
}

/* ✅ 모바일 퍼스트 보강: 작은 화면에서 헤더 줄바꿈 */
@media (max-width: 430px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .meta-info {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
