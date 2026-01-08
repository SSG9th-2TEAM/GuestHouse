<script setup>
import { computed } from 'vue'

const props = defineProps({
  id: [Number, String],
  title: String,
  description: String,
  location: String,
  rating: [Number, String],
  reviewCount: [Number, String],
  price: Number,
  imageUrl: String,
  isFavorite: Boolean,
  isActive: {
    type: Boolean,
    default: true
  }
})

// 썸네일 URL 생성 (원본 URL에서 폴더명에 _thumb 추가)
const DEFAULT_IMAGE = 'https://placehold.co/400x300?text=No+Image'

const thumbnailUrl = computed(() => {
  if (!props.imageUrl) return DEFAULT_IMAGE
  
  // Object Storage URL인 경우 썸네일 폴더로 변경
  if (props.imageUrl.includes('ncloudstorage.com')) {
    // accommodation_image -> accommodation_image_thumb
    // room -> room_thumb
    let thumbUrl = props.imageUrl
      .replace('/accommodation_image/', '/accommodation_image_thumb/')
      .replace('/room/', '/room_thumb/')
    
    // 확장자를 .jpg로 변경 (썸네일은 모두 jpg)
    thumbUrl = thumbUrl.replace(/\.(png|gif|webp)$/i, '.jpg')
    
    return thumbUrl
  }
  
  // 다른 URL은 원본 그대로 사용
  return props.imageUrl
})

const formatRating = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '-'
  return numeric.toFixed(2)
}

const hasReviewCount = computed(() => {
  const numeric = Number(props.reviewCount)
  return Number.isFinite(numeric) && numeric > 0
})

const formatReviewCount = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return ''
  return numeric.toLocaleString()
}

const emit = defineEmits(['toggle-favorite'])
</script>

<template>
  <article class="card" :class="{ inactive: !isActive }">
    <div class="image-container">
      <img :src="thumbnailUrl" :alt="title" class="card-image" />
      <span v-if="!isActive" class="inactive-badge">사용 중지</span>
      <button
        class="favorite-btn"
        :class="{ active: isFavorite }"
        @click.stop="$emit('toggle-favorite', id)"
      >
        <span v-if="isFavorite">&#9829;</span>
        <span v-else>&#9825;</span>
      </button>
    </div>
    <div class="card-content">
      <div class="header-row">
        <h3 class="title">{{ title }}</h3>
      </div>
      <div class="card-bottom">
        <p v-if="description" class="description">{{ description }}</p>
        <p class="location">{{ location }}</p>
        <div class="footer-row">
          <span class="price">
            <strong>&#8361;{{ price.toLocaleString() }}</strong> / 1박
          </span>
          <span class="rating">
            <span class="rating-value">&#9733; {{ formatRating(rating) }}</span>
            <span v-if="hasReviewCount" class="rating-count">({{ formatReviewCount(reviewCount) }})</span>
          </span>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
/* ... existing styles ... */
</style>

<style scoped>
.card {
  background: var(--bg-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.image-container {
  aspect-ratio: 1 / 1;
  background: #e5e7eb;
  position: relative;
  overflow: hidden;
  width: 100%;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  /* 이미지 축소 시 품질 개선 */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: smooth;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  transform: translateZ(0);
  /* 부드러운 렌더링 */
  filter: blur(0);
  -webkit-filter: blur(0);
}

.favorite-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255,255,255,0.8);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  cursor: pointer;
  border: none;
  transition: color 0.2s, transform 0.1s;
}

.favorite-btn.active {
  color: #ef4444; /* Red Heart */
}

.inactive-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #ef4444;
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.card.inactive {
  opacity: 0.6;
}

.favorite-btn:active {
  transform: scale(0.9);
}

.card-content {
  padding: 0.15rem 0.5rem 0.35rem;
  display: flex;
  flex-direction: column;
  gap: 0.02rem;
  flex: 1;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
  flex: 1;
  min-width: 0;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.rating-count {
  font-size: 0.85em;
  color: var(--text-sub);
}

.description {
  font-size: 0.82rem;
  color: var(--text-sub);
  line-height: 1.1;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.location {
  font-size: 0.86rem;
  color: var(--text-sub);
  line-height: 1;
  margin: 0;
}

.card-bottom {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 0.02rem;
}

.footer-row {
  margin-top: 0.15rem;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.book-btn {
  background-color: var(--primary);
  color: #004d40;
  padding: 0.2rem 0.55rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  transition: opacity 0.2s;
  border: none;
}

.book-btn:hover {
  opacity: 0.9;
}

/* Mobile specific styles */
@media (max-width: 768px) {
  .rating {
    flex-direction: column;
    align-items: flex-end;
    gap: 0;
  }

  .rating-value {
    font-size: 0.85rem;
  }

  .rating-count {
    font-size: 0.75rem;
    color: var(--text-sub);
  }

  .footer-row {
    align-items: flex-end;
  }
}
</style>

