<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchList } from '@/api/list'
import FilterModal from '../../components/FilterModal.vue'
import { useSearchStore } from '@/stores/search'
import { matchesKeyword } from '@/utils/searchFilter'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const items = ref([])
const selectedItem = ref(null)
const mapContainer = ref(null)
const mapInstance = ref(null)
const activeOverlays = ref([])

// Filter State
const isFilterModalOpen = ref(false)
const minPrice = ref(null)
const maxPrice = ref(null)
const selectedThemeIds = ref([])

const parseNumberParam = (value) => {
  if (value === undefined || value === null || value === '') return null
  const raw = Array.isArray(value) ? value[0] : value
  const numberValue = Number(raw)
  return Number.isFinite(numberValue) ? numberValue : null
}

const parseThemeIds = (value) => {
  if (!value) return []
  const raw = Array.isArray(value) ? value.join(',') : String(value)
  return raw
    .split(',')
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item))
}

const applyRouteFilters = () => {
  minPrice.value = parseNumberParam(route.query.min ?? route.query.minPrice)
  maxPrice.value = parseNumberParam(route.query.max ?? route.query.maxPrice)
  selectedThemeIds.value = parseThemeIds(route.query.themeIds)
}

const getKeywordFromRoute = () => {
  const raw = route.query.keyword
  if (Array.isArray(raw)) {
    return raw[0] ?? ''
  }
  return raw ?? ''
}

const applyRouteKeyword = () => {
  const nextKeyword = getKeywordFromRoute()
  if (nextKeyword !== searchStore.keyword) {
    searchStore.setKeyword(nextKeyword)
  }
}

const normalizeItem = (item) => {
  const id = item.accomodationsId ?? item.accommodationsId ?? item.id
  const title = item.accomodationsName ?? item.accommodationsName ?? item.title ?? ''
  const description = item.shortDescription ?? item.description ?? ''
  const location = [item.city, item.district, item.township].filter(Boolean).join(' ')
  const price = Number(item.minPrice ?? item.price ?? 0)
  const rating = item.rating ?? null
  const reviewCount = item.reviewCount ?? null
  const imageUrl = item.imageUrl || 'https://placehold.co/400x300'
  const lat = Number(item.latitude ?? item.lat)
  const lng = Number(item.longitude ?? item.lng)
  return {
    id,
    title,
    description,
    location,
    price,
    rating,
    reviewCount,
    imageUrl,
    lat: Number.isFinite(lat) ? lat : null,
    lng: Number.isFinite(lng) ? lng : null
  }
}

const loadList = async (themeIds = []) => {
  try {
    const response = await fetchList(themeIds)
    if (response.ok) {
      const payload = response.data
      const list = Array.isArray(payload)
        ? payload
        : payload?.items ?? payload?.content ?? payload?.data ?? []
      items.value = list.map(normalizeItem)
      updateMarkers()
    } else {
      console.error('Failed to load list', response.status)
    }
  } catch (error) {
    console.error('Failed to load list', error)
  }
}

const handleMarkerClick = (item) => {
  selectedItem.value = item
}

const closeCard = () => {
  selectedItem.value = null
}

const goToDetail = (id) => {
  if (!id) return
  const query = { from: 'map', ...buildFilterQuery() }
  router.push({ path: `/room/${id}`, query })
}

const updateMarkers = () => {
  if (!mapInstance.value) return

  // Clear existing markers
  activeOverlays.value.forEach(overlay => overlay.setMap(null))
  activeOverlays.value = []

  // Filter items
  const filteredItems = items.value.filter(item => {
    if (minPrice.value !== null && item.price < minPrice.value) return false
    if (maxPrice.value !== null && item.price > maxPrice.value) return false
    return matchesKeyword(item, searchStore.keyword)
  })
  const itemsWithCoords = filteredItems.filter(
    (item) => Number.isFinite(item.lat) && Number.isFinite(item.lng) && Number.isFinite(item.price) && item.price > 0
  )

  if (selectedItem.value) {
    const nextSelected = itemsWithCoords.find(item => item.id === selectedItem.value.id)
    selectedItem.value = nextSelected || null
  }

  // Create new markers
  itemsWithCoords.forEach(item => {
    const position = new window.kakao.maps.LatLng(item.lat, item.lng)

    // Custom Overlay Content
    const content = document.createElement('div')
    content.className = 'price-marker'
    content.innerHTML = `₩${item.price.toLocaleString()}`
    
    content.onclick = () => {
      handleMarkerClick(item)
    }

    // Creating Custom Overlay
    const customOverlay = new window.kakao.maps.CustomOverlay({
      position: position,
      content: content,
      yAnchor: 1 
    })

    customOverlay.setMap(mapInstance.value)
    activeOverlays.value.push(customOverlay)
  })

  if (!itemsWithCoords.length) return

  if (itemsWithCoords.length === 1) {
    mapInstance.value.setCenter(
      new window.kakao.maps.LatLng(itemsWithCoords[0].lat, itemsWithCoords[0].lng)
    )
    mapInstance.value.setLevel(5)
    return
  }

  const bounds = new window.kakao.maps.LatLngBounds()
  itemsWithCoords.forEach((item) => {
    bounds.extend(new window.kakao.maps.LatLng(item.lat, item.lng))
  })
  mapInstance.value.setBounds(bounds)
}

const handleApplyFilter = ({ min, max, themeIds = [] }) => {
  minPrice.value = min
  maxPrice.value = max
  selectedThemeIds.value = themeIds
  isFilterModalOpen.value = false
  loadList(themeIds)
}

const buildFilterQuery = () => {
  const query = {}
  if (minPrice.value !== null) query.min = String(minPrice.value)
  if (maxPrice.value !== null) query.max = String(maxPrice.value)
  if (selectedThemeIds.value.length) query.themeIds = selectedThemeIds.value.join(',')
  const keyword = (searchStore.keyword || '').trim()
  if (keyword) query.keyword = keyword
  return query
}

const goToList = () => {
  router.push({ path: '/list', query: buildFilterQuery() })
}

onMounted(() => {
  applyRouteFilters()
  applyRouteKeyword()
  if (!window.kakao?.maps?.load) {
    console.error('Kakao Maps SDK not loaded')
    return
  }

  window.kakao.maps.load(() => {
    if (!mapContainer.value) return

    const options = {
      center: new window.kakao.maps.LatLng(36.5, 127.8), // Center of South Korea approximate
      level: 13 // Zoom level to see the whole country
    }

    mapInstance.value = new window.kakao.maps.Map(mapContainer.value, options)

    // Initial render
    loadList(selectedThemeIds.value)
  })
})

watch(
  () => route.query.keyword,
  () => {
    applyRouteKeyword()
    updateMarkers()
  }
)
</script>

<template>
  <div class="map-wrapper">
    <!-- Map Container -->
    <div ref="mapContainer" class="map-container"></div>

    <!-- Filter Button -->
    <div class="filter-btn-wrapper">
      <button class="filter-floating-btn" @click="isFilterModalOpen = true">
        <span class="icon">🔍</span>
        <span class="text">필터</span>
      </button>
    </div>

    <!-- Floating List Button -->
    <div class="list-btn-wrapper">
      <button class="list-floating-btn" @click="goToList">
        <span class="text">리스트에서 보기</span>
      </button>
    </div>

    <!-- Filter Modal -->
    <FilterModal 
      :is-open="isFilterModalOpen"
      :current-min="minPrice"
      :current-max="maxPrice"
      :current-themes="selectedThemeIds"
      @close="isFilterModalOpen = false"
      @apply="handleApplyFilter"
    />

    <div v-if="selectedItem" class="map-card">
      <button class="map-card-close" type="button" @click="closeCard" aria-label="닫기">×</button>
      <div class="map-card-content" @click="goToDetail(selectedItem.id)">
        <img
          v-if="selectedItem.imageUrl"
          :src="selectedItem.imageUrl"
          :alt="selectedItem.title"
          class="map-card-image"
        />
        <div class="map-card-body">
          <div class="map-card-title">{{ selectedItem.title }}</div>
          <div class="map-card-location">{{ selectedItem.location }}</div>
          <div v-if="selectedItem.description" class="map-card-description">
            {{ selectedItem.description }}
          </div>
          <div class="map-card-meta">
            <span class="map-card-price">₩{{ selectedItem.price.toLocaleString() }}</span>
            <span
              v-if="selectedItem.rating !== null && selectedItem.rating !== undefined"
              class="map-card-rating"
            >
              ★ {{ selectedItem.rating.toFixed(2) }} ({{ selectedItem.reviewCount || 0 }})
            </span>
          </div>
          <button class="map-card-action" type="button" @click.stop="goToDetail(selectedItem.id)">
            상세보기
          </button>
        </div>
      </div>
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

/* Filter Button Styles */
.filter-btn-wrapper {
  position: absolute;
  top: 1rem;
  right: 2rem;
  z-index: 50;
}

.filter-floating-btn {
  background-color: white;
  color: #222;
  border: 1px solid #ddd;
  border-radius: 24px;
  padding: 10px 18px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 0.95rem;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s, background-color 0.2s;
}

.filter-floating-btn:hover {
  background-color: #f9f9f9;
  transform: scale(1.05);
}

.map-card {
  position: fixed;
  bottom: 6rem;
  left: 50%;
  transform: translateX(-50%);
  width: min(480px, calc(100% - 2rem));
  background: white;
  border-radius: 18px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  z-index: 60;
  overflow: hidden;
}

.map-card-content {
  display: flex;
  gap: 12px;
  padding: 14px;
  cursor: pointer;
}

.map-card-image {
  width: 112px;
  height: 112px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f3f4f6;
}

.map-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.map-card-title {
  font-size: 1rem;
  font-weight: 700;
  color: #111;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.map-card-location {
  font-size: 0.9rem;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.map-card-description {
  font-size: 0.85rem;
  color: #4b5563;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.map-card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 0.9rem;
  color: #111;
}

.map-card-price {
  font-weight: 700;
}

.map-card-rating {
  font-weight: 600;
}

.map-card-action {
  margin-top: 4px;
  align-self: flex-start;
  background: #111;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 6px 12px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.map-card-action:hover {
  background: #000;
}

.map-card-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.map-card-close:hover {
  background: rgba(0, 0, 0, 0.15);
}
</style>
