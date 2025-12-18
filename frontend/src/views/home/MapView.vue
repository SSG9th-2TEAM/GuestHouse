<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { guesthouses } from '../../data/guesthouses'

const router = useRouter()
const items = guesthouses
const mapContainer = ref(null)

onMounted(() => {
  if (!window.kakao || !window.kakao.maps) {
    console.error('Kakao Maps SDK not loaded')
    return
  }

  const options = {
    center: new window.kakao.maps.LatLng(36.5, 127.8), // Center of South Korea approximate
    level: 13 // Zoom level to see the whole country
  }

  const map = new window.kakao.maps.Map(mapContainer.value, options)

  // Create markers (CustomOverlays for price tags)
  items.forEach(item => {
    if (!item.lat || !item.lng) return

    const position = new window.kakao.maps.LatLng(item.lat, item.lng)

    // Custom Overlay Content
    const content = document.createElement('div')
    content.className = 'price-marker'
    content.innerHTML = `₩${(item.price / 10000).toLocaleString()}만`
    
    content.onclick = () => {
      router.push(`/room/${item.id}`)
    }

    // Creating Custom Overlay
    const customOverlay = new window.kakao.maps.CustomOverlay({
      position: position,
      content: content,
      yAnchor: 1 
    })

    customOverlay.setMap(map)
  })
})
</script>

<template>
  <div class="map-wrapper">
    <!-- Map Container -->
    <div ref="mapContainer" class="map-container"></div>

    <!-- Floating List Button -->
    <div class="list-btn-wrapper">
      <button class="list-floating-btn" @click="router.push('/list')">
        <span class="icon">≣</span>
        <span class="text">목록 보기</span>
      </button>
    </div>
  </div>
</template>

<style>
/* Global styles for the keys injected into the map */
.price-marker {
  background-color: white;
  padding: 8px 12px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 0.9rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  cursor: pointer;
  color: #111;
  border: 1px solid rgba(0,0,0,0.05);
  transition: transform 0.2s, background-color 0.2s;
  white-space: nowrap;
}

.price-marker:hover {
  transform: scale(1.1);
  background-color: #222;
  color: white;
  z-index: 100;
}
</style>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100%;
  height: calc(100vh - 80px); /* Adjust for header */
  background-color: #f0f0f0;
}

.map-container {
  width: 100%;
  height: 100%;
}

/* Floating Button Styles */
.list-btn-wrapper {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 50;
}

.list-floating-btn {
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
  transition: transform 0.2s;
}

.list-floating-btn:hover {
  background-color: #000;
  transform: scale(1.05);
}

.list-floating-btn .icon {
  font-size: 1.2rem;
  line-height: 1;
}
</style>
