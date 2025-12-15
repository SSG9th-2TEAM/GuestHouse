<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Mock data
const wishlist = ref([
  {
    id: 1,
    title: '제주 카약 체험',
    location: '제주시 조천읍',
    rating: 4.8,
    image: 'https://picsum.photos/id/11/300/200'
  },
  {
    id: 6,
    title: '북촌 한옥마을 투어',
    location: '서울시 종로구',
    rating: 4.6,
    image: 'https://picsum.photos/id/42/300/200'
  },
  {
    id: 2,
    title: '속초 맛집 투어',
    location: '강원도 속초시',
    rating: 4.9,
    image: 'https://picsum.photos/id/15/300/200'
  },
  {
    id: 3,
    title: '경주 역사 탐방',
    location: '경상북도 경주시',
    rating: 4.7,
    image: 'https://picsum.photos/id/18/300/200'
  }
])

const goToDetail = (id) => {
  router.push(`/room/${id}`)
}
</script>

<template>
  <div class="wishlist-page container">
    <h1 class="page-title">위시리스트</h1>

    <div v-if="wishlist.length === 0" class="empty-state">
      위시리스트가 비어있습니다.
    </div>

    <div v-else class="wish-grid">
      <div 
        v-for="item in wishlist" 
        :key="item.id" 
        class="wish-card"
        @click="goToDetail(item.id)"
      >
        <div class="card-image-wrapper">
          <img :src="item.image" alt="thumbnail" class="card-img" />
          <button class="heart-btn">
            <span class="heart-icon">♥</span>
          </button>
        </div>
        
        <div class="card-info">
          <div class="row-top">
            <h3 class="title">{{ item.title }}</h3>
            <span class="rating">★ {{ item.rating }}</span>
          </div>
          <p class="location">{{ item.location }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wishlist-page {
  padding-top: 2rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 2rem;
}

.empty-state {
  text-align: center;
  padding: 4rem 0;
  color: #888;
  font-size: 1.1rem;
}

.wish-grid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.wish-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #eee;
  cursor: pointer;
  transition: transform 0.2s;
}

.wish-card:hover {
  transform: translateY(-2px);
}

.card-image-wrapper {
  position: relative;
  height: 200px;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.heart-btn {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  cursor: pointer;
}

.heart-icon {
  color: var(--primary); /* Theme Color Heart */
  font-size: 1.2rem;
  line-height: 1;
}

.card-info {
  padding: 1.2rem;
}

.row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.title {
  font-size: 1.1rem;
  font-weight: 800;
  color: #333;
}

.rating {
  font-size: 0.95rem;
  font-weight: bold;
  color: #333;
}

.location {
  font-size: 0.9rem;
  color: #666;
}
</style>
