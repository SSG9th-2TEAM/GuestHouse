<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { searchList } from '@/api/list'
import FilterModal from '../../components/FilterModal.vue'
import { useSearchStore } from '@/stores/search'
import { useListingFilters } from '@/composables/useListingFilters'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const { applyRouteFilters, buildFilterQuery } = useListingFilters()
const items = ref([])
const selectedItem = ref(null)
const mapContainer = ref(null)
const mapInstance = ref(null)
const activeOverlays = ref([])
const isLoading = ref(false)
const isMapVisible = ref(false)

const MAP_PAGE_SIZE = 200
const MAX_MAP_RESULTS = 600
const AUTO_FIT_SINGLE_LEVEL = 6
const AUTO_FIT_ZOOM_IN_LEVELS = 1
const AUTO_FIT_MIN_LEVEL = 6
const AUTO_FIT_MAX_LEVEL_FOR_ZOOM_IN = 9

// Filter State
const isFilterModalOpen = ref(false)

let requestId = 0
let lastQueryKey = ''
let pendingLoad = null
let idleTimer = null
let autoFitPending = true
let initialLoadDone = false
let allowGlobalFallback = true

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
  const maxGuestsValue = Number(item.maxGuests ?? item.capacity ?? item.maxGuest ?? 0)
  const maxGuests = Number.isFinite(maxGuestsValue) ? maxGuestsValue : 0
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
    maxGuests,
    lat: Number.isFinite(lat) ? lat : null,
    lng: Number.isFinite(lng) ? lng : null
  }
}

const getBounds = () => {
  if (!mapInstance.value) return null
  const bounds = mapInstance.value.getBounds()
  if (!bounds) return null
  const southWest = bounds.getSouthWest()
  const northEast = bounds.getNorthEast()
  return {
    minLat: southWest.getLat(),
    maxLat: northEast.getLat(),
    minLng: southWest.getLng(),
    maxLng: northEast.getLng()
  }
}

const formatDateKey = (value) => {
  if (!value) return ''
  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) return ''
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  return String(value).trim()
}

const buildQueryKey = (bounds, themeIds, keyword, guestCount, checkin, checkout) => {
  const themeKey = [...(themeIds || [])].sort((a, b) => a - b).join(',')
  const safeKeyword = String(keyword ?? '').trim()
  const guestKey = Number.isFinite(guestCount) ? String(guestCount) : ''
  const checkinKey = formatDateKey(checkin)
  const checkoutKey = formatDateKey(checkout)
  const format = (value) => Number(value).toFixed(4)
  return [
    format(bounds.minLat),
    format(bounds.maxLat),
    format(bounds.minLng),
    format(bounds.maxLng),
    themeKey,
    safeKeyword,
    guestKey,
    checkinKey,
    checkoutKey
  ].join('|')
}

const fetchAllPages = async ({
  themeIds = [],
  keyword = searchStore.keyword,
  bounds,
  checkin = searchStore.startDate,
  checkout = searchStore.endDate,
  guestCount = searchStore.guestCount
}) => {
  const allItems = []
  let page = 0

  while (true) {
    const response = await searchList({
      themeIds,
      keyword,
      checkin,
      checkout,
      guestCount,
      page,
      size: MAP_PAGE_SIZE,
      bounds
    })
    if (!response.ok) {
      console.error('Failed to load list', response.status)
      break
    }
    const list = Array.isArray(response.data?.items) ? response.data.items : []
    allItems.push(...list)
    const pageInfo = response.data?.page
    if (!pageInfo?.hasNext) break
    if (allItems.length >= MAX_MAP_RESULTS) break
    page += 1
  }

  return allItems
}

const loadList = async ({
  themeIds = searchStore.themeIds,
  keyword = searchStore.keyword,
  bounds,
  checkin = searchStore.startDate,
  checkout = searchStore.endDate,
  guestCount = searchStore.guestCount,
  queryKey
} = {}) => {
  if (!bounds) return
  if (isLoading.value) {
    pendingLoad = { themeIds, keyword, bounds, checkin, checkout, guestCount, queryKey }
    return
  }

  const currentRequest = ++requestId
  isLoading.value = true
  try {
    let list = await fetchAllPages({ themeIds, keyword, bounds, checkin, checkout, guestCount })
    if (currentRequest !== requestId) return
    if (!list.length && bounds && allowGlobalFallback) {
      const fallbackList = await fetchAllPages({ themeIds, keyword, checkin, checkout, guestCount })
      if (currentRequest !== requestId) return
      if (fallbackList.length) {
        list = fallbackList
      }
    }
    items.value = list.map(normalizeItem)
    const itemsWithCoords = updateMarkers()
    fitToMarkers(itemsWithCoords)
    if (!initialLoadDone) {
      initialLoadDone = true
      isMapVisible.value = true
    }
    if (queryKey) {
      lastQueryKey = queryKey
    }
    allowGlobalFallback = false
  } catch (error) {
    console.error('Failed to load list', error)
  } finally {
    if (currentRequest === requestId) {
      isLoading.value = false
    }
    if (currentRequest === requestId && !initialLoadDone) {
      initialLoadDone = true
      isMapVisible.value = true
    }
    if (pendingLoad && currentRequest === requestId) {
      const nextLoad = pendingLoad
      pendingLoad = null
      loadList(nextLoad)
    }
  }
}

const scheduleLoad = ({
  themeIds = searchStore.themeIds,
  keyword = searchStore.keyword,
  checkin = searchStore.startDate,
  checkout = searchStore.endDate,
  guestCount = searchStore.guestCount
} = {}) => {
  const bounds = getBounds()
  if (!bounds) return
  const queryKey = buildQueryKey(bounds, themeIds, keyword, guestCount, checkin, checkout)
  if (queryKey === lastQueryKey) return

  pendingLoad = { themeIds, keyword, bounds, checkin, checkout, guestCount, queryKey }
  if (idleTimer) {
    clearTimeout(idleTimer)
  }
  idleTimer = setTimeout(() => {
    if (!pendingLoad) return
    const nextLoad = pendingLoad
    pendingLoad = null
    loadList(nextLoad)
  }, 250)
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

const syncMarkerActiveState = () => {
  const selectedId = selectedItem.value?.id
  activeOverlays.value.forEach(({ element, itemId }) => {
    if (!element) return
    const isActive = selectedId !== null && selectedId !== undefined && String(itemId) === String(selectedId)
    element.classList.toggle('price-marker--active', isActive)
  })
}

const updateMarkers = () => {
  if (!mapInstance.value) return

  // Clear existing markers
  activeOverlays.value.forEach(({ overlay }) => overlay.setMap(null))
  activeOverlays.value = []

  const itemsWithCoords = getItemsWithCoords()

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
    const isSelected = selectedItem.value && String(selectedItem.value.id) === String(item.id)
    if (isSelected) {
      content.classList.add('price-marker--active')
    }
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
    activeOverlays.value.push({ overlay: customOverlay, element: content, itemId: item.id })
  })

  syncMarkerActiveState()
  return itemsWithCoords
}

const getItemsWithCoords = () => {
  const minValue = searchStore.minPrice
  const maxValue = searchStore.maxPrice
  const guestCount = searchStore.guestCount
  const filteredItems = items.value.filter(item => {
    if (minValue !== null && item.price < minValue) return false
    if (maxValue !== null && item.price > maxValue) return false
    if (guestCount > 0 && item.maxGuests < guestCount) return false
    return true
  })

  return filteredItems.filter(
    (item) => Number.isFinite(item.lat) && Number.isFinite(item.lng) && Number.isFinite(item.price) && item.price > 0
  )
}

const fitToMarkers = (itemsWithCoords) => {
  if (!mapInstance.value || !autoFitPending) return
  if (!itemsWithCoords || itemsWithCoords.length === 0) return

  autoFitPending = false
  if (itemsWithCoords.length === 1) {
    const target = itemsWithCoords[0]
    mapInstance.value.setCenter(new window.kakao.maps.LatLng(target.lat, target.lng))
    const currentLevel = mapInstance.value.getLevel()
    const nextLevel = Math.min(currentLevel, AUTO_FIT_SINGLE_LEVEL)
    mapInstance.value.setLevel(nextLevel)
    return
  }

  const bounds = new window.kakao.maps.LatLngBounds()
  itemsWithCoords.forEach(item => {
    bounds.extend(new window.kakao.maps.LatLng(item.lat, item.lng))
  })
  mapInstance.value.setBounds(bounds)
  const currentLevel = mapInstance.value.getLevel()
  if (currentLevel <= AUTO_FIT_MIN_LEVEL || currentLevel > AUTO_FIT_MAX_LEVEL_FOR_ZOOM_IN) return
  const nextLevel = Math.max(1, currentLevel - AUTO_FIT_ZOOM_IN_LEVELS)
  if (nextLevel !== currentLevel) {
    mapInstance.value.setLevel(nextLevel)
  }
}

const handleApplyFilter = ({ min, max, themeIds = [], guestCount = 0 }) => {
  searchStore.setPriceRange(min, max)
  searchStore.setThemeIds(themeIds)
  searchStore.setGuestCount(guestCount)
  autoFitPending = true
  isFilterModalOpen.value = false
  updateMarkers()
  scheduleLoad({ themeIds })
}

const goToList = () => {
  router.push({ path: '/list', query: buildFilterQuery() })
}

onMounted(() => {
  applyRouteFilters(route.query)
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

    const disableAutoFit = () => {
      autoFitPending = false
    }

    window.kakao.maps.event.addListener(mapInstance.value, 'dragstart', disableAutoFit)
    window.kakao.maps.event.addListener(mapInstance.value, 'zoom_start', disableAutoFit)

    window.kakao.maps.event.addListener(mapInstance.value, 'idle', () => {
      scheduleLoad()
    })

    // Initial render
    scheduleLoad()
  })
})

watch(
  () => [
    route.query.keyword,
    route.query.themeIds,
    route.query.min,
    route.query.max,
    route.query.minPrice,
    route.query.maxPrice,
    route.query.guestCount,
    route.query.checkin,
    route.query.checkout
  ],
  () => {
    applyRouteFilters(route.query)
    applyRouteKeyword()
    autoFitPending = true
    allowGlobalFallback = true
    scheduleLoad()
  }
)

watch(
  () => selectedItem.value?.id,
  () => {
    syncMarkerActiveState()
  }
)

watch(
  () => [searchStore.minPrice, searchStore.maxPrice, searchStore.guestCount],
  () => {
    updateMarkers()
  }
)
</script>

<template>
  <div class="map-wrapper">
    <!-- Map Container -->
    <div ref="mapContainer" class="map-container" :class="{ 'map-container--hidden': !isMapVisible }"></div>

    <!-- Filter Button -->
    <div class="filter-btn-wrapper">
      <button class="filter-floating-btn" @click="isFilterModalOpen = !isFilterModalOpen">
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
      :current-min="searchStore.minPrice"
      :current-max="searchStore.maxPrice"
      :current-themes="searchStore.themeIds"
      :current-guest-count="searchStore.guestCount"
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

.price-marker--active {
  transform: scale(1.07);
  background-color: #222;
  color: white;
  z-index: 110;
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

.map-container--hidden {
  opacity: 0;
  pointer-events: none;
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
