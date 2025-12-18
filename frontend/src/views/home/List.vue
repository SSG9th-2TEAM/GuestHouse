<script setup>
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import { useRouter } from 'vue-router'
import { guesthouses } from '../../data/guesthouses'

const router = useRouter()
const items = guesthouses
</script>

<template>
  <main class="container main-content">
    <div class="header">
      <h1>숙소 목록</h1>
    </div>

    <div class="list-container">
      <GuesthouseCard 
        v-for="item in items" 
        :key="item.id"
        :title="item.title"
        :location="item.location"
        :price="item.price"
        :image-url="item.imageUrl"
        @click="router.push(`/room/${item.id}`)"
        class="list-item"
      />
    </div>

    <!-- Floating Map Button -->
    <div class="map-btn-wrapper">
      <button class="map-floating-btn" @click="router.push('/map')">
        <span class="icon">🗺️</span>
        <span class="text">지도에서 보기</span>
      </button>
    </div>
  </main>
</template>

<style scoped>
.main-content {
  padding-top: 2rem;
  padding-bottom: 6rem; /* Extra padding for floating button */
  max-width: 1200px; 
  margin: 0 auto;
  padding-left: 1rem;
  padding-right: 1rem;
}

.header {
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.header h1 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-main);
}

.list-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); 
  gap: 2rem;
}

.list-item {
  cursor: pointer;
  transition: transform 0.2s;
}

.list-item:hover {
  transform: translateY(-5px);
}

/* Floating Button Styles */
.map-btn-wrapper {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 50;
}

.map-floating-btn {
  background-color: #222;
  color: white;
  border: none;
  border-radius: 24px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
  cursor: pointer;
  transition: transform 0.2s, background-color 0.2s;
}

.map-floating-btn:hover {
  background-color: #000;
  transform: scale(1.05);
}

.map-floating-btn .icon {
  font-size: 1.1rem;
}
</style>
