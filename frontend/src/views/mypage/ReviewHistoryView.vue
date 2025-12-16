<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Mock data
const reviews = ref([
  {
    id: 1,
    accommodationId: 1,
    accommodationName: '제주 서핑 체험',
    location: '제주시 한림읍',
    rating: 5,
    date: '2025.11.28',
    content: '정말 즐거운 체험이었어요! 강사님이 친절하게 가르쳐주셔서 초보인 저도 금방 배울 수 있었습니다. 제주 바다도 너무 아름답고 시설도 깨끗해서 만족스러웠어요. 다음에 또 방문하고 싶습니다.',
    images: [
      'https://picsum.photos/id/11/200/150',
      'https://picsum.photos/id/15/200/150',
      'https://picsum.photos/id/18/200/150'
    ],
    tags: ['친절해요', '깨끗해요', '위치가 좋아요'],
    helpfulCount: 12
  },
  {
    id: 2,
    accommodationId: 6,
    accommodationName: '북촌 한옥마을 투어',
    location: '서울시 종로구',
    rating: 4,
    date: '2025.11.15',
    content: '한옥마을의 아름다움을 제대로 느낄 수 있었습니다. 가이드 분이 역사적 배경을 자세히 설명해주셔서 유익했어요. 다만 주말이라 사람이 많아서 조금 복잡했어요.',
    images: [],
    tags: ['가이드가 좋아요', '유익해요'],
    helpfulCount: 8
  },
  {
    id: 3,
    accommodationId: 2,
    accommodationName: '속초 맛집 투어',
    location: '강원도 속초시',
    rating: 5,
    date: '2025.10.30',
    content: '속초의 숨은 맛집들을 다 가볼 수 있어서 좋았어요! 특히 물회랑 닭강정이 정말 맛있었습니다. 혼자서는 찾기 힘든 곳들을 가이드와 함께 가니까 더 좋았어요.',
    images: [
      'https://picsum.photos/id/42/200/150'
    ],
    tags: ['맛있어요', '가성비 좋아요', '친절해요'],
    helpfulCount: 24
  }
])

const goToDetail = (id) => {
  router.push(`/room/${id}`)
}

const renderStars = (rating) => {
  return '★'.repeat(rating) + '☆'.repeat(5 - rating)
}
</script>

<template>
  <div class="review-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>리뷰 내역</h1>
    </div>

    <!-- Empty State -->
    <div v-if="reviews.length === 0" class="empty-state">
      <div class="empty-icon">☆</div>
      <h2>아직 작성한 리뷰가 없어요</h2>
      <p>숙소를 이용한 후 리뷰를 남겨보세요.</p>
      <p>다른 여행자들에게 도움이 될 수 있어요.</p>
    </div>

    <!-- Review List -->
    <div v-else class="review-list">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <!-- Top Row: Accommodation Info -->
        <div class="review-top">
          <img :src="review.images[0] || 'https://picsum.photos/id/100/60/60'" class="review-thumb" />
          <div class="review-info">
            <h3 class="acc-name" @click="goToDetail(review.accommodationId)">
              {{ review.accommodationName }}
            </h3>
            <span class="acc-location">{{ review.location }}</span>
            <div class="rating-row">
              <span class="stars">{{ renderStars(review.rating) }}</span>
              <span class="date">{{ review.date }}</span>
            </div>
          </div>
          <button class="more-btn">⋮</button>
        </div>

        <!-- Review Content -->
        <p class="review-content">{{ review.content }}</p>

        <!-- Review Tags -->
        <div v-if="review.tags && review.tags.length > 0" class="review-tags">
          <span v-for="tag in review.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>

        <!-- Review Images -->
        <div v-if="review.images && review.images.length > 0" class="review-images">
          <img v-for="(img, idx) in review.images" :key="idx" :src="img" class="review-img" />
        </div>

        <!-- Helpful Count -->
        <div class="helpful-row">
          <span class="helpful-icon">👍</span>
          <span class="helpful-text">도움이 됐어요 {{ review.helpfulCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
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

/* Empty State */
.empty-state {
  text-align: center;
  padding: 6rem 2rem;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 50%;
  margin: 0 auto 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: #bbb;
}

.empty-state h2 {
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: #333;
}

.empty-state p {
  color: #888;
  font-size: 0.95rem;
  line-height: 1.6;
}

/* Review List */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.review-card {
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.review-top {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  align-items: flex-start;
}

.review-thumb {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  object-fit: cover;
  background: #eee;
}

.review-info {
  flex: 1;
}

.acc-name {
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  color: #333;
}

.acc-name:hover {
  color: var(--primary);
  text-decoration: underline;
}

.acc-location {
  font-size: 0.85rem;
  color: #888;
  display: block;
  margin-bottom: 4px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stars {
  color: #fbbf24;
  font-size: 0.9rem;
}

.date {
  font-size: 0.85rem;
  color: #888;
}

.more-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #888;
  cursor: pointer;
}

.review-content {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #333;
  margin-bottom: 1rem;
}

/* Review Tags */
.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tag {
  background: var(--primary);
  color: #004d40;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 0.8rem;
  font-weight: 500;
}

/* Review Images */
.review-images {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  overflow-x: auto;
}

.review-img {
  width: 100px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

/* Helpful */
.helpful-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #2563eb;
  font-size: 0.9rem;
}

.helpful-icon {
  font-size: 1rem;
}
</style>
