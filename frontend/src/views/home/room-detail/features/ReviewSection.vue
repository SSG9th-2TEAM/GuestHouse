<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  reviews: {
    type: Array,
    default: () => []
  },
  name: {
    type: String,
    default: ''
  }
})

const REVIEWS_PER_PAGE = 5
const showAllReviewTags = ref(false)
const currentReviewPage = ref(1)
const expandedReviewIds = ref([])

const reviewTagSummary = computed(() => {
  const counts = new Map()
  props.reviews.forEach((review) => {
    const tags = Array.isArray(review?.tags) ? review.tags : []
    tags.forEach((tag) => {
      if (!tag) return
      const key = String(tag).trim()
      if (!key) return
      counts.set(key, (counts.get(key) || 0) + 1)
    })
  })
  return Array.from(counts.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
})

const visibleReviewTags = computed(() => {
  if (showAllReviewTags.value) return reviewTagSummary.value
  return reviewTagSummary.value.slice(0, 4)
})

const hasMoreReviewTags = computed(() => reviewTagSummary.value.length > 4)

const totalReviewPages = computed(() => {
  const count = Math.ceil(props.reviews.length / REVIEWS_PER_PAGE)
  return count || 1
})

const pagedReviews = computed(() => {
  const start = (currentReviewPage.value - 1) * REVIEWS_PER_PAGE
  return props.reviews.slice(start, start + REVIEWS_PER_PAGE)
})

const reviewPageNumbers = computed(() => {
  const total = totalReviewPages.value
  const current = currentReviewPage.value

  if (total <= 5) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }

  if (current <= 2) {
    return [1, 2, 3, 'ellipsis', total]
  }

  if (current === 3) {
    return [1, 2, 3, 4, 'ellipsis', total]
  }

  if (current >= total - 1) {
    return [1, 'ellipsis', total - 2, total - 1, total]
  }

  return [1, 'ellipsis', current - 1, current, current + 1, 'ellipsis', total]
})

const toggleReviewTags = () => {
  showAllReviewTags.value = !showAllReviewTags.value
}

const goToReviewPage = (page) => {
  const target = Math.min(Math.max(page, 1), totalReviewPages.value)
  currentReviewPage.value = target
}

const isReviewExpanded = (id) => expandedReviewIds.value.includes(id)

const toggleReviewExpanded = (id) => {
  if (isReviewExpanded(id)) {
    expandedReviewIds.value = expandedReviewIds.value.filter((value) => value !== id)
    return
  }
  expandedReviewIds.value = [...expandedReviewIds.value, id]
}

const isReviewLong = (content) => {
  return String(content ?? '').length > 120
}

const renderStars = (rating) => {
  const safeRating = Math.min(Math.max(Number(rating) || 0, 0), 5)
  return '★'.repeat(safeRating) + '☆'.repeat(5 - safeRating)
}

const isReviewImageModalOpen = ref(false)
const selectedReviewImageIndex = ref(0)
const reviewModalImages = ref([])

const currentReviewModalImage = computed(() => {
  return reviewModalImages.value[selectedReviewImageIndex.value] || ''
})

const openReviewImageModal = (images, index) => {
  const list = Array.isArray(images) ? images : []
  if (!list.length) return
  const maxIndex = Math.max(0, list.length - 1)
  reviewModalImages.value = list
  selectedReviewImageIndex.value = Math.min(Math.max(index, 0), maxIndex)
  isReviewImageModalOpen.value = true
}

const closeReviewImageModal = () => {
  isReviewImageModalOpen.value = false
}

const showPrevReviewImage = () => {
  const total = reviewModalImages.value.length
  if (!total) return
  selectedReviewImageIndex.value = (selectedReviewImageIndex.value - 1 + total) % total
}

const showNextReviewImage = () => {
  const total = reviewModalImages.value.length
  if (!total) return
  selectedReviewImageIndex.value = (selectedReviewImageIndex.value + 1) % total
}

watch(
  () => props.reviews,
  () => {
    currentReviewPage.value = 1
    expandedReviewIds.value = []
    showAllReviewTags.value = false
    isReviewImageModalOpen.value = false
  }
)

watch(
  () => props.reviews.length,
  () => {
    if (currentReviewPage.value > totalReviewPages.value) {
      currentReviewPage.value = totalReviewPages.value
    }
  }
)
</script>

<template>
  <section class="section reviews-section">
    <h2>리뷰</h2>
    <div v-if="reviewTagSummary.length" class="review-tag-summary">
      <div class="review-tag-summary-header">
        <div class="review-tag-list">
          <span
            v-for="tag in visibleReviewTags"
            :key="tag.name"
            class="tag review-tag summary-tag"
          >
            {{ tag.name }}
            <span class="tag-count">{{ tag.count }}</span>
          </span>
        </div>
        <button
          v-if="hasMoreReviewTags"
          type="button"
          class="review-tag-toggle"
          @click="toggleReviewTags"
        >
          {{ showAllReviewTags ? '접기' : '더보기' }}
        </button>
      </div>
    </div>
    <div class="review-list">
      <div v-for="review in pagedReviews" :key="review.id" class="review-item">
        <div class="review-header">
          <span class="author">{{ review.author }}</span>
          <span class="date">{{ review.date }}</span>
        </div>
        <div class="stars">{{ renderStars(review.rating) }}</div>
        <div v-if="review.images && review.images.length" class="review-images">
          <img
            v-for="(img, idx) in review.images.slice(0, 5)"
            :key="`${review.id}-img-${idx}`"
            :src="img"
            class="review-img"
            loading="lazy"
            role="button"
            tabindex="0"
            @click="openReviewImageModal(review.images, idx)"
            @keydown.enter.prevent="openReviewImageModal(review.images, idx)"
          />
        </div>
        <p class="review-content" :class="{ clamped: !isReviewExpanded(review.id) }">
          {{ review.content }}
        </p>
        <button
          v-if="isReviewLong(review.content)"
          type="button"
          class="review-content-toggle"
          @click="toggleReviewExpanded(review.id)"
        >
          {{ isReviewExpanded(review.id) ? '접기' : '더보기' }}
        </button>
        <div v-if="review.tags && review.tags.length" class="review-tags">
          <span v-for="tag in review.tags" :key="tag" class="tag review-tag">{{ tag }}</span>
        </div>
      </div>
    </div>
    <div v-if="totalReviewPages > 1" class="review-pagination">
      <button
        type="button"
        class="review-page-btn nav"
        :disabled="currentReviewPage === 1"
        @click="goToReviewPage(currentReviewPage - 1)"
      >
        이전
      </button>
      <div class="review-page-list">
        <template v-for="page in reviewPageNumbers" :key="`page-${page}`">
          <span v-if="page === 'ellipsis'" class="review-page-ellipsis">…</span>
          <button
            v-else
            type="button"
            class="review-page-btn number"
            :class="{ active: page === currentReviewPage }"
            :aria-current="page === currentReviewPage ? 'page' : null"
            @click="goToReviewPage(page)"
          >
            {{ page }}
          </button>
        </template>
      </div>
      <button
        type="button"
        class="review-page-btn nav"
        :disabled="currentReviewPage === totalReviewPages"
        @click="goToReviewPage(currentReviewPage + 1)"
      >
        다음
      </button>
    </div>
  </section>

  <div v-if="isReviewImageModalOpen" class="image-modal" @click="closeReviewImageModal">
    <div class="image-modal-content" @click.stop>
      <div class="image-modal-header">
        <button type="button" class="image-modal-close" @click="closeReviewImageModal">닫기</button>
      </div>
      <div class="image-modal-body">
        <button
          v-if="reviewModalImages.length > 1"
          type="button"
          class="image-modal-nav prev"
          @click="showPrevReviewImage"
          aria-label="이전 사진"
        >
          ‹
        </button>
        <img :src="currentReviewModalImage" :alt="name" class="image-modal-image" />
        <button
          v-if="reviewModalImages.length > 1"
          type="button"
          class="image-modal-nav next"
          @click="showNextReviewImage"
          aria-label="다음 사진"
        >
          ›
        </button>
      </div>
      <div class="image-modal-caption">{{ selectedReviewImageIndex + 1 }} / {{ reviewModalImages.length }}</div>
    </div>
  </div>
</template>

<style scoped>
.section {
  padding: 1.5rem 0;
}
h2 {
  font-size: 1.4rem;
  margin-bottom: 1rem;
}
.tag {
  background: #f3f4f6;
  color: var(--text-main);
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.85rem;
}
.review-stats {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}
.stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
}
.review-item {
  padding: 1rem 0;
  border-bottom: 1px solid #eee;
}
.review-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  margin-bottom: 0.3rem;
}
.date {
  color: var(--text-sub);
}
.stars {
  margin-bottom: 0.5rem;
}
.review-tag-summary {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.review-tag-summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
}
.review-tag-list {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  gap: 0.4rem;
  min-width: 0;
}
.summary-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
.tag-count {
  font-size: 0.75rem;
  font-weight: 700;
  color: #0f4c44;
}
.review-tag-toggle {
  margin-left: auto;
  flex-shrink: 0;
  white-space: nowrap;
  background: #BFE7DF;
  border: 1px solid #8FCFC1;
  color: #0f4c44;
  font-weight: 700;
  cursor: pointer;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
}
.review-content {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #333;
  margin-bottom: 0.5rem;
}
.review-content.clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.review-content-toggle {
  background: none;
  border: none;
  color: var(--text-sub);
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0;
  margin-bottom: 0.5rem;
}
.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.5rem;
}
.review-tag {
  background: #e6f4f1;
  color: #0f4c44;
}
.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.review-img {
  width: calc((100% - 1.5rem) / 4);
  height: auto;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  margin-top: 0;
  object-fit: cover;
}
.review-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 1rem;
}
.review-page-list {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.review-page-btn {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  padding: 0.35rem 0.9rem;
  font-size: 0.85rem;
  cursor: pointer;
  color: var(--text-main);
}
.review-page-btn.number {
  min-width: 36px;
  height: 36px;
  padding: 0 0.65rem;
  font-weight: 600;
  background: #fff;
}
.review-page-btn.number.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #004d40;
}
.review-page-btn.nav {
  background: #fff;
  border-color: #e5e7eb;
  font-weight: 600;
}
.review-page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.review-page-ellipsis {
  color: var(--text-sub);
  font-size: 0.9rem;
  padding: 0 0.25rem;
}

.image-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 0;
}
.image-modal-content {
  background: #fff;
  border-radius: 0;
  padding: 0;
  width: 100%;
  max-width: 100%;
  max-height: 100vh;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.image-modal-body {
  display: flex;
  align-items: center;
  gap: 0;
  position: relative;
  justify-content: center;
  width: 100%;
}
.image-modal-header {
  display: flex;
  justify-content: flex-end;
  padding: 0.5rem 0.75rem;
  background: rgba(255, 255, 255, 0.92);
}
.image-modal-image {
  width: 100%;
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 0;
  background: #f3f4f6;
  display: block;
}
.image-modal-close {
  background: #BFE7DF;
  border: 1px solid #8FCFC1;
  color: #0f4c44;
  font-weight: 700;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  cursor: pointer;
}
.image-modal-nav {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
}
.image-modal-nav.prev {
  left: 8px;
}
.image-modal-nav.next {
  right: 8px;
}
.image-modal-caption {
  text-align: center;
  color: var(--text-sub);
  font-size: 0.85rem;
  padding: 0.5rem 0;
}

@media (max-width: 768px) {
  .review-img {
    width: calc((100% - 1rem) / 3);
  }

  .image-modal-content {
    width: 100%;
  }
  .image-modal-image {
    max-height: 60vh;
  }
  .image-modal-nav {
    width: 32px;
    height: 32px;
    font-size: 1.2rem;
  }
  .image-modal-nav.prev {
    left: 4px;
  }
  .image-modal-nav.next {
    right: 4px;
  }
}
</style>
