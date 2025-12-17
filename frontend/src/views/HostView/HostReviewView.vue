<script setup>
import {ref, computed} from 'vue'

// Mock Data for Reviews
const reviews = ref([
  {
    id: 1,
    userName: '김민수',
    userInitial: '김',
    accommodationName: '제주도 감성 숙소',
    rating: 5,
    date: '2024-12-08',
    content: '정말 깨끗하고 호스트님이 친절하셨어요. 제주 여행 다시 오면 또 이용하고 싶습니다!',
    reply: '',
    showReplyForm: true
  },
  {
    id: 2,
    userName: '이서연',
    userInitial: '이',
    accommodationName: '부산 해운대 오션뷰',
    rating: 5,
    date: '2024-12-07',
    content: '바다뷰가 정말 환상적이었어요. 위치도 좋고 시설도 깔끔했습니다.',
    reply: '',
    showReplyForm: false
  },
  {
    id: 3,
    userName: '박지성',
    userInitial: '박',
    accommodationName: '서울 강남 레지던스',
    rating: 4,
    date: '2024-12-05',
    content: '위치는 좋았는데 편의시설이 조금 부족했어요. 그래도 전반적으로 만족합니다.',
    reply: '소중한 후기 감사합니다. 편의시설 보완하도록 하겠습니다.',
    showReplyForm: false
  },
  {
    id: 4,
    userName: '최현우',
    userInitial: '최',
    accommodationName: '경주 한옥 스테이',
    rating: 5,
    date: '2024-12-01',
    content: '고즈넉한 분위기가 너무 좋았습니다. 부모님 모시고 갔는데 정말 좋아하셨어요.',
    reply: '',
    showReplyForm: false
  },
  {
    id: 5,
    userName: '정수민',
    userInitial: '정',
    accommodationName: '강릉 오션뷰 펜션',
    rating: 4,
    date: '2024-11-28',
    content: '뷰는 끝내주는데 방음이 살짝 아쉬웠어요.',
    reply: '',
    showReplyForm: false
  }
])

const averageRating = computed(() => {
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

const submitReply = (reviewId) => {
  const review = reviews.value.find(r => r.id === reviewId)
  if (review && replyText.value[reviewId]) {
    review.reply = replyText.value[reviewId]
    review.showReplyForm = false
    alert('답변이 등록되었습니다.')
  }
}

const cancelReply = (reviewId) => {
  const review = reviews.value.find(r => r.id === reviewId)
  if (review) {
    review.showReplyForm = false
    replyText.value[reviewId] = ''
  }
}
</script>

<template>
  <div class="review-view">
    <div class="view-header">
      <h2>리뷰 관리</h2>
      <p class="subtitle">평균 평점 {{ averageRating }} · 총 {{ reviews.length }}개의 리뷰</p>
    </div>

    <div class="review-list">
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
              <button class="btn-submit" @click="submitReply(review.id)">답변 등록</button>
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
          <button class="btn-report">
            <span class="icon">🚩</span> 신고하기
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
  color: #0b3b32;
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
  border: 1px solid #e5e7eb;
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
  background: #E0F2F1;
  color: #0f766e;
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
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 0.95rem;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
  font-weight: 600;
}

.reply-form textarea:focus {
  outline: none;
  border-color: #c0e6df;
  box-shadow: 0 0 0 3px rgba(191, 231, 223, 0.45);
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
  border: 1px solid #e5e7eb;
  color: #475569;
}

.btn-submit {
  background: #0f766e;
  border: none;
  color: white;
}

.btn-reply-toggle {
  background: none;
  border: none;
  color: #0f766e;
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
