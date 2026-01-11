<template>
  <div class="event-carousel">
    <div class="carousel-container">
      <!-- 배너 이미지들 -->
      <div 
        class="carousel-track" 
        :style="{ transform: `translateX(-${currentIndex * 100}%)` }"
      >
        <div 
          v-for="(banner, index) in banners" 
          :key="index" 
          class="carousel-slide"
          :class="{ 'clickable': banner.link }"
          @click="handleBannerClick(banner)"
        >
          <img :src="banner.image" :alt="banner.alt" class="banner-image" />
        </div>
      </div>

      <!-- 왼쪽 화살표 -->
      <button 
        class="carousel-arrow arrow-left" 
        @click="prevSlide"
        aria-label="이전 배너"
      >
        ‹
      </button>

      <!-- 오른쪽 화살표 -->
      <button 
        class="carousel-arrow arrow-right" 
        @click="nextSlide"
        aria-label="다음 배너"
      >
        ›
      </button>

      <!-- 인디케이터 (점) -->
      <div class="carousel-indicators">
        <button
          v-for="(banner, index) in banners"
          :key="`indicator-${index}`"
          class="indicator"
          :class="{ active: index === currentIndex }"
          @click="goToSlide(index)"
          :aria-label="`${index + 1}번 배너로 이동`"
        ></button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 배너 이미지 목록
const banners = [
  { 
    image: new URL('@/assets/banners/event1.png', import.meta.url).href, 
    alt: '좋은 사람, 좋은 장소, 좋은 시간',
    link: null // 클릭해도 이동 안 함
  },
  { 
    image: new URL('@/assets/banners/event2.png', import.meta.url).href, 
    alt: '3박 이상 장기숙박 할인 - 여름 특가 쿠폰',
    link: '/events' // 이벤트 페이지로 이동
  },
  { 
    image: new URL('@/assets/banners/event3.png', import.meta.url).href, 
    alt: '오늘의 선착순 50 - 7천원 즉시 할인',
    link: '/events' // 이벤트 페이지로 이동
  }
]

const currentIndex = ref(0)
let autoSlideInterval = null

// 배너 클릭 핸들러
const handleBannerClick = (banner) => {
  if (banner.link) {
    router.push(banner.link)
  }
}

// 다음 슬라이드
const nextSlide = () => {
  currentIndex.value = (currentIndex.value + 1) % banners.length
}

// 이전 슬라이드
const prevSlide = () => {
  currentIndex.value = (currentIndex.value - 1 + banners.length) % banners.length
}

// 특정 슬라이드로 이동
const goToSlide = (index) => {
  currentIndex.value = index
  resetAutoSlide()
}

// 자동 슬라이드 시작
const startAutoSlide = () => {
  autoSlideInterval = setInterval(() => {
    nextSlide()
  }, 5000) // 5초마다 자동 전환
}

// 자동 슬라이드 리셋 (사용자가 화살표 클릭 시)
const resetAutoSlide = () => {
  if (autoSlideInterval) {
    clearInterval(autoSlideInterval)
  }
  startAutoSlide()
}

// 컴포넌트 마운트 시 자동 슬라이드 시작
onMounted(() => {
  startAutoSlide()
})

// 컴포넌트 언마운트 시 자동 슬라이드 정리
onUnmounted(() => {
  if (autoSlideInterval) {
    clearInterval(autoSlideInterval)
  }
})
</script>

<style scoped>
.event-carousel {
  width: 100%;
  margin-bottom: 2rem;
}

.carousel-container {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.carousel-track {
  display: flex;
  transition: transform 0.5s ease-in-out;
}

.carousel-slide {
  min-width: 100%;
  flex-shrink: 0;
}

.carousel-slide.clickable {
  cursor: pointer;
}

.carousel-slide.clickable:hover .banner-image {
  opacity: 0.9;
  transition: opacity 0.3s ease;
}

.banner-image {
  width: 100%;
  height: 300px;
  display: block;
  object-fit: cover;
  object-position: center;
}

/* 화살표 버튼 */
.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.9);
  border: none;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  font-size: 2rem;
  font-weight: bold;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.carousel-arrow:hover {
  background: white;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.arrow-left {
  left: 1rem;
}

.arrow-right {
  right: 1rem;
}

/* 인디케이터 (점) */
.carousel-indicators {
  position: absolute;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.75rem;
  z-index: 10;
}

.indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 0;
}

.indicator:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: scale(1.2);
}

.indicator.active {
  background: white;
  width: 32px;
  border-radius: 6px;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .carousel-arrow {
    width: 40px;
    height: 40px;
    font-size: 1.5rem;
  }

  .arrow-left {
    left: 0.5rem;
  }

  .arrow-right {
    right: 0.5rem;
  }

  .carousel-indicators {
    bottom: 1rem;
  }

  .indicator {
    width: 10px;
    height: 10px;
  }

  .indicator.active {
    width: 24px;
  }

  .banner-image {
    height: 180px;
  }
}
</style>
