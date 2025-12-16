<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Mock Data
const upcomingReservations = ref([
  {
    id: 1,
    title: '제주 서핑 캠프',
    location: '제주 서귀포',
    date: '2025.12.25',
    time: '14:00',
    guests: 2,
    price: 35000,
    image: 'https://picsum.photos/id/16/200/200'
  },
  {
    id: 2,
    title: '한옥 스테이',
    location: '서울 종로구',
    date: '2026.01.05',
    time: '15:00',
    guests: 4,
    price: 120000,
    image: 'https://picsum.photos/id/18/200/200'
  }
])

const pastReservations = ref([
  {
    id: 3,
    title: '로맨틱 루프',
    location: '강릉 동명항',
    date: '2025.11.30',
    time: '09:00',
    guests: 2,
    price: 35000,
    image: 'https://picsum.photos/id/42/200/200'
  }
])

const handleDelete = (id) => {
  if(confirm('내역에서 삭제하시겠습니까?')) {
    // Logic to remove...
    upcomingReservations.value = upcomingReservations.value.filter(r => r.id !== id)
    pastReservations.value = pastReservations.value.filter(r => r.id !== id)
  }
}
</script>

<template>
  <div class="reservation-page container">
    <div class="header-section">
      <button class="back-btn" @click="router.back()">←</button>
      <h1 class="page-title">예약 내역</h1>
    </div>

    <!-- Upcoming Reservations -->
    <section class="section">
      <h2 class="section-title">예정된 예약</h2>
      
      <div v-if="upcomingReservations.length === 0" class="empty-state">
        예정된 예약이 없습니다.
      </div>

      <div v-else class="card-list">
        <div v-for="item in upcomingReservations" :key="item.id" class="res-card">
          <div class="card-content">
            <img :src="item.image" class="card-img" alt="thumbnail" />
            <div class="card-info">
              <h3 class="res-title">{{ item.title }}</h3>
              <p class="res-loc"> {{ item.location }}</p>
              <div class="res-details">
                <span>날짜</span> <span class="val">{{ item.date }}</span>
                <span class="spacer">시간</span> <span class="val">{{ item.time }}</span>
              </div>
              <div class="res-details">
                <span>인원</span> <span class="val">{{ item.guests }}명</span>
              </div>
              <div class="res-price">
                결제금액 <span class="price-val">{{ item.price.toLocaleString() }}원</span>
              </div>
            </div>
          </div>
          
          <div class="card-actions">
            <button class="action-btn dark">예약 변경</button>
            <button class="action-btn outline" @click="router.push({
              name: 'reservation-cancel', 
              params: { id: item.id },
              state: { 
                reservationData: {
                  id: item.id,
                  hotelName: item.title,
                  location: item.location,
                  dates: item.date,
                  guests: item.guests,
                  price: item.price,
                  image: item.image
                }
              }
            })">예약 취소</button>
            <button class="icon-btn delete" @click="handleDelete(item.id)">🗑</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Past Reservations -->
    <section class="section">
      <h2 class="section-title">이용 완료</h2>

      <div v-if="pastReservations.length === 0" class="empty-state">
        이용 완료된 내역이 없습니다.
      </div>

      <div v-else class="card-list">
        <div v-for="item in pastReservations" :key="item.id" class="res-card">
          <div class="card-content">
            <img :src="item.image" class="card-img" alt="thumbnail" />
            <div class="card-info">
              <h3 class="res-title">{{ item.title }}</h3>
              <p class="res-loc"> {{ item.location }}</p>
              <div class="res-details">
                <span>날짜</span> <span class="val">{{ item.date }}</span>
                <span class="spacer">시간</span> <span class="val">{{ item.time }}</span>
              </div>
              <div class="res-details">
                <span>인원</span> <span class="val">{{ item.guests }}명</span>
              </div>
              <div class="res-price">
                결제금액 <span class="price-val">{{ item.price.toLocaleString() }}원</span>
              </div>
            </div>
          </div>
          
          <div class="card-actions">
            <button class="action-btn gray-full" @click="router.push({
              name: 'write-review',
              state: {
                reservationData: {
                  accommodationName: item.title,
                  dates: item.date
                }
              }
            })">리뷰 작성하기</button>
            <button class="icon-btn delete" @click="handleDelete(item.id)">🗑</button>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.reservation-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.header-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-title {
  font-size: 1.3rem;
  font-weight: 700;
}

.section {
  margin-bottom: 2.5rem;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 800;
  margin-bottom: 1rem;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #888;
  background: #f9f9f9;
  border-radius: 12px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.res-card {
  background: white;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.card-content {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.card-img {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  object-fit: cover;
  background: #eee;
}

.card-info {
  flex: 1;
}

.res-title {
  font-size: 1rem;
  font-weight: 800;
  margin-bottom: 0.3rem;
  color: #111;
}

.res-loc {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.res-details {
  font-size: 0.85rem;
  color: #444;
  margin-bottom: 2px;
}
.res-details .spacer {
  margin-left: 8px;
}
.res-details .val {
  font-weight: 500;
}

.res-price {
  margin-top: 0.5rem;
  font-size: 0.9rem;
  color: #0066ff; /* Blue for price keyword as in screenshot? Wait, label is blue? No, usually price is blue. Screenshot shows "결제금액" is blue text or the value is blue. Looking at screenshot 2: "결제금액" is Blue, Value is Blue. */
  font-weight: bold;
}

.price-val {
  color: #2563eb;
}

/* Actions */
.card-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  flex: 1;
  padding: 0.6rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
}

.action-btn.dark {
  background: var(--primary);
  color: #004d40;
  border: 1px solid var(--primary);
}

.action-btn.outline {
  background: white;
  color: #555;
  border: 1px solid #ddd;
}

.action-btn.gray-full {
  background: #e5e7eb;
  color: #374151; /* Dark gray text */
  border: none;
}

.icon-btn.delete {
  background: var(--primary);
  border: 1px solid var(--primary);
  color: #e11d48; /* Red Icon */
  width: 42px; /* Square-ish */
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  cursor: pointer;
}

</style>
