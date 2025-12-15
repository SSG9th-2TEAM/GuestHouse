<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()

import { guesthouses } from '../../data/guesthouses'

const route = useRoute()
const id = parseInt(route.params.id) || 1
const basicInfo = guesthouses.find(item => item.id === id) || guesthouses[0]

const guesthouse = {
  id: basicInfo.id,
  name: basicInfo.title,
  rating: 4.7,
  reviewCount: 89,
  address: basicInfo.location,
  description: '북촌 한옥마을 인근에 위치한 아늑한 게스트하우스입니다. 전통과 현대가 조화를 이루는 편안한 공간에서 특별한 경험을 제공합니다.',
  host: {
    name: '김민수',
    joined: '2020년 1월 가입',
    image: 'https://picsum.photos/id/64/100/100'
  },
  images: [
    basicInfo.imageUrl,
    'https://picsum.photos/id/15/400/300',
    'https://picsum.photos/id/18/400/300',
    'https://picsum.photos/id/29/400/300',
    'https://picsum.photos/id/28/400/300'
  ],
  rooms: [
    { id: 101, name: '스탠다드 더블룸', desc: '더블 침대가 있는 기본 객실', capacity: 2, price: basicInfo.price, available: true },
    { id: 102, name: '디럭스 트윈룸', desc: '싱글 침대 2개가 있는 넓은 객실', capacity: 2, price: basicInfo.price + 15000, available: true },
    { id: 103, name: '패밀리룸', desc: '더블 침대와 싱글 침대가 있는 가족 객실', capacity: 4, price: basicInfo.price + 70000, available: false }
  ],
  reviews: [
    { id: 1, author: '김철수', date: '2024-11-15', rating: 5, content: '정말 깨끗하고 친절한 호스트입니다. 다시 방문하고 싶네요!', image: 'https://picsum.photos/id/101/100/100' },
    { id: 2, author: '이순신', date: '2024-11-10', rating: 4, content: '위치가 좋고 편의시설이 잘 갖춰져 있습니다. 추천합니다.', image: 'https://picsum.photos/id/102/100/100' },
    { id: 3, author: '박민지', date: '2024-11-05', rating: 5, content: '조용하고 쾌적한 환경에서 좋은 시간을 보냈습니다.', image: 'https://picsum.photos/id/103/100/100' }
  ]
}

const guestCount = ref(2)
const selectedRoom = ref(null)

const selectRoom = (room) => {
  selectedRoom.value = room
}

const formatPrice = (price) => {
  return price.toLocaleString()
}
</script>

<template>
  <div class="room-detail container">
    <!-- Header with Back Button -->
    <div class="detail-header">
      <button class="back-btn" @click="router.push('/')">← 뒤로가기</button>
    </div>

    <!-- Image Grid -->
    <div class="image-grid">
      <div class="main-img" :style="{ backgroundImage: `url(${guesthouse.images[0]})` }"></div>
      <div class="sub-imgs">
        <div v-for="(img, idx) in guesthouse.images.slice(1, 5)" :key="idx" 
             class="sub-img" :style="{ backgroundImage: `url(${img})` }"></div>
      </div>
    </div>

    <!-- Title & Info -->
    <section class="section info-section">
      <h1>{{ guesthouse.name }}</h1>
      <div class="meta">
        <span class="rating">★ {{ guesthouse.rating }} (후기 {{ guesthouse.reviewCount }}개)</span>
        <span class="location">{{ guesthouse.address }}</span>
      </div>
      <p class="description">{{ guesthouse.description }}</p>
    </section>

    <hr />

    <!-- Host -->
    <section class="section host-section">
      <div class="host-info">
        <h3>호스트: {{ guesthouse.host.name }}</h3>
        <p class="join-date">{{ guesthouse.host.joined }}</p>
      </div>
      <img :src="guesthouse.host.image" alt="Host Profile" class="host-avatar" />
    </section>

    <hr />

    <!-- Room Selection -->
    <section class="section room-selection">
      <h2>객실 선택</h2>
      
      <!-- Date & Guest Picker Mock -->
      <div class="picker-box">
        <div class="picker-row">
          <div class="picker-field">
            <label>체크인 / 체크아웃</label>
            <div class="date-display">2024-12-20 - 2024-12-22</div>
          </div>
          <div class="picker-field">
            <label>투숙 인원</label>
            <div class="guest-control">
              <button @click="guestCount > 1 && guestCount--" :disabled="guestCount <= 1">-</button>
              <span>성인 {{ guestCount }}</span>
              <button @click="guestCount < 10 && guestCount++">+</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Room List -->
      <div class="room-list">
        <div v-for="room in guesthouse.rooms" :key="room.id" 
             class="room-card" 
             :class="{ selected: selectedRoom?.id === room.id }"
             @click="selectRoom(room)">
          <div class="room-info">
            <h3>{{ room.name }}</h3>
            <p>{{ room.desc }}</p>
            <span class="capacity">최대 {{ room.capacity }}명</span>
          </div>
          <div class="room-action">
            <div class="price">₩{{ formatPrice(room.price) }}</div>
            <button class="select-btn" :class="{ active: selectedRoom?.id === room.id }">
              {{ selectedRoom?.id === room.id ? '선택됨' : '객실' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <hr />

    <!-- Reviews -->
    <section class="section reviews-section">
      <h2>후기</h2>
      <div class="review-stats">
        <!-- Mock stats based on image -->
        <div class="stat-row"><span>❤️ 친절해요</span> <span>316</span></div>
        <div class="stat-row"><span>⭐ 깨끗해요</span> <span>265</span></div>
        <div class="stat-row"><span>🛌 침구가 좋아요</span> <span>216</span></div>
      </div>
      
      <div class="review-list">
        <div v-for="review in guesthouse.reviews" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="author">{{ review.author }}</span>
            <span class="date">{{ review.date }}</span>
          </div>
          <div class="stars">{'⭐'.repeat(review.rating)}</div>
          <p class="content">{{ review.content }}</p>
          <img v-if="review.image" :src="review.image" class="review-img" />
        </div>
      </div>
    </section>

    <!-- Map Placeholder -->
    <section class="section map-section">
      <h2>숙소 위치</h2>
      <div class="map-placeholder">
        [지도] {{ guesthouse.address }}
      </div>
      <p class="mt-2">북촌 한옥마을에서 도보 5분, 가까운 지하철역: 안국역(3호선)</p>
    </section>

    <!-- Rules -->
    <section class="section rules-section">
      <div class="rule-box">
        <h3>환불 규정</h3>
        <ul>
          <li>체크인 7일 전까지 취소: 100% 환불</li>
          <li>체크인 3일 전까지 취소: 50% 환불</li>
          <li>체크인 3일 이내 취소: 환불 불가</li>
        </ul>
      </div>
      <div class="rule-box mt-4">
        <h3>이용 규칙</h3>
        <ul>
          <li>체크인: 오후 3시 이후</li>
          <li>체크아웃: 오전 11시까지</li>
          <li>흡연 금지</li>
          <li>반려동물 동반 불가</li>
        </ul>
      </div>
    </section>

    <!-- Disclaimer / Bottom Spacer -->
    <div style="height: 100px;"></div>

    <!-- Floating Bottom Bar -->
    <div class="bottom-bar">
      <div class="selection-summary">
        <span v-if="selectedRoom">선택한 객실: {{ selectedRoom.name }}</span>
        <span v-else>객실을 선택해주세요</span>
        <div class="total-price" v-if="selectedRoom">₩{{ formatPrice(selectedRoom.price) }}</div>
      </div>
      <button class="book-btn" :disabled="!selectedRoom" @click="$router.push('/booking/1')">예약하기</button>
    </div>

  </div>
</template>

<style scoped>
.room-detail {
  padding-bottom: 2rem;
  max-width: 800px;
}

.detail-header {
  padding: 1rem 0;
  margin-bottom: 0.5rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1rem;
  color: #333;
  cursor: pointer;
  padding: 0.5rem 0;
}

.back-btn:hover {
  color: var(--primary);
}

/* Image Grid */
.image-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  grid-template-rows: 200px 200px;
  gap: 8px;
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: 2rem;
}
.main-img {
  grid-row: 1 / span 2;
  background-size: cover;
  background-position: center;
}
.sub-imgs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 8px;
}
.sub-img {
  background-size: cover;
  background-position: center;
}

/* Sections */
.section {
  padding: 1.5rem 0;
}
hr {
  border: 0;
  border-top: 1px solid #eee;
  margin: 0;
}
h1 { font-size: 1.8rem; margin-bottom: 0.5rem; }
h2 { font-size: 1.4rem; margin-bottom: 1rem; }
h3 { font-size: 1.1rem; margin-bottom: 0.5rem; }

/* Info */
.meta {
  color: var(--text-sub);
  margin-bottom: 1rem;
  font-size: 0.95rem;
}
.description { line-height: 1.6; }

/* Host */
.host-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.host-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

/* Room Selection */
.picker-box {
  border: 1px solid #ddd;
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}
.picker-row {
  display: flex;
  gap: 1rem;
}
.picker-field { flex: 1; }
.picker-field label { display: block; font-size: 0.8rem; font-weight: bold; margin-bottom: 0.5rem; }
.date-display, .guest-control {
  border: 1px solid #ddd;
  padding: 0.8rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.guest-control button {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
}

/* Room Card */
.room-card {
  border: 1px solid #ddd;
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 1rem;
  display: flex;
  justify-content: space-between;
  cursor: pointer;
  transition: border-color 0.2s;
}
.room-card:hover { border-color: var(--primary); }
.room-card.selected { border: 2px solid var(--primary); background-color: #f9fdfc; }
.room-info h3 { margin-bottom: 0.3rem; }
.room-info p { color: var(--text-sub); font-size: 0.9rem; margin-bottom: 0.5rem; }
.capacity { font-size: 0.8rem; background: #eee; padding: 2px 6px; border-radius: 4px; }
.room-action { text-align: right; display: flex; flex-direction: column; justify-content: space-between; }
.price { font-weight: bold; font-size: 1.1rem; }
.select-btn {
  padding: 0.5rem 1rem;
  background: #eee;
  border-radius: 8px;
}
.select-btn.active {
  background: var(--primary);
  color: #000;
}

/* Reviews */
.review-stats {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}
.stat-row { display: flex; justify-content: space-between; font-size: 0.9rem; }
.review-item { padding: 1rem 0; border-bottom: 1px solid #eee; }
.review-header { display: flex; justify-content: space-between; font-size: 0.9rem; margin-bottom: 0.3rem; }
.date { color: var(--text-sub); }
.stars { margin-bottom: 0.5rem; }
.review-img { width: 80px; height: 80px; border-radius: 8px; margin-top: 0.5rem; object-fit: cover; }

/* Map */
.map-placeholder {
  background: #eee;
  height: 200px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
}

/* Rules */
.rule-box h3 { margin-bottom: 0.8rem; }
.rule-box ul { list-style: inside disc; color: var(--text-sub); font-size: 0.9rem; line-height: 1.6; }
.mt-2 { margin-top: 0.5rem; }
.mt-4 { margin-top: 1rem; }

/* Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid #ddd;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  z-index: 100;
}
.selection-summary { display: flex; flex-direction: column; }
.total-price { font-weight: bold; font-size: 1.2rem; }
.book-btn {
  background: var(--primary);
  color: #004d40;
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-weight: bold;
  font-size: 1rem;
}
.book-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .image-grid { grid-template-columns: 1fr; grid-template-rows: 200px auto; }
  .sub-imgs { display: none; } /* Hide sub images on mobile for simplicity */
}
</style>
