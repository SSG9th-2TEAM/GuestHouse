<script setup>
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import FilterModal from '../../components/FilterModal.vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchList } from '@/api/list'
import { ref, computed, onMounted } from 'vue'

import { fetchWishlistIds, addWishlist, removeWishlist } from '@/api/wishlist'
import { isAuthenticated } from '@/api/authClient'

const router = useRouter()
const route = useRoute()
const items = ref([])
const wishlistIds = ref(new Set())

// Filter State
const isFilterModalOpen = ref(false)
const minPrice = ref(null)
const maxPrice = ref(null)
const selectedThemeIds = ref([])

const normalizeItem = (item) => {
  const id = item.accomodationsId ?? item.accommodationsId ?? item.id
  const title = item.accomodationsName ?? item.accommodationsName ?? item.title ?? ''
  const description = item.shortDescription ?? item.description ?? ''
  const rating = item.rating ?? null
  const location = [item.city, item.district, item.township].filter(Boolean).join(' ')
  const price = Number(item.minPrice ?? item.price ?? 0)
  const imageUrl = item.imageUrl || 'https://via.placeholder.com/400x300'
  return { id, title, description, rating, location, price, imageUrl }
}

const loadWishlist = async () => {
  try {
    const res = await fetchWishlistIds()
    if (res.status === 200 && Array.isArray(res.data)) {
      wishlistIds.value = new Set(res.data)
    }
  } catch (e) {
    console.error('Failed to load wishlist', e)
  }
}

const toggleWishlist = async (id) => {
  if (!isAuthenticated()) {
    if (confirm('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동하시겠습니까?')) {
      router.push('/login')
    }
    return
  }

  const isAdded = wishlistIds.value.has(id)
  if (isAdded) {
    wishlistIds.value.delete(id)
    try {
      await removeWishlist(id)
    } catch (e) {
      wishlistIds.value.add(id)
      console.error(e)
    }
  } else {
    wishlistIds.value.add(id)
    try {
      await addWishlist(id)
    } catch (e) {
      wishlistIds.value.delete(id)
      console.error(e)
    }
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
    } else {
      console.error('Failed to load list', response.status)
    }
  } catch (error) {
    console.error('Failed to load list', error)
  }
}

// Computed Items
const filteredItems = computed(() => {
  return items.value.filter(item => {
    if (minPrice.value !== null && item.price < minPrice.value) return false
    if (maxPrice.value !== null && item.price > maxPrice.value) return false
    return true
  })
})

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
  return query
}

const goToMap = () => {
  router.push({ path: '/map', query: buildFilterQuery() })
}

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

onMounted(() => {
  loadWishlist()
  applyRouteFilters()
  if (selectedThemeIds.value.length) {
    loadList(selectedThemeIds.value)
    return
  }
  loadList()
})
</script>

<template>
  <main class="container main-content">
    <div class="header">
      <h1>숙소 목록</h1>
      <button class="filter-btn" @click="isFilterModalOpen = true"><span class="icon">🔍</span>필터</button>
    </div>

    <div class="list-container">
      <GuesthouseCard
        v-for="item in filteredItems"
        :key="item.id"
        :id="item.id"
        :title="item.title"
        :description="item.description"
        :rating="item.rating"
        :location="item.location"
        :price="item.price"
        :image-url="item.imageUrl"
        :is-favorite="wishlistIds.has(item.id)"
        @toggle-favorite="toggleWishlist"
        @click="router.push(`/room/${item.id}`)"
        class="list-item"
      />
    </div>

    <!-- Floating Map Button -->
    <div class="map-btn-wrapper">
      <button class="map-floating-btn" @click="goToMap">
        <span class="text">지도에서 보기</span>
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
  </main>
</template>

<style scoped>
.main-content {
  padding-top: 1rem;
  padding-bottom: 6rem; /* Extra padding for floating button */
  max-width: 1280px; 
  margin: 0 auto;
  padding-left: 1rem;
  padding-right: 1rem;
}

.header {
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-btn:hover {
  background-color: #f5f5f5;
}

.list-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--card-width, 280px), var(--card-width, 280px))); 
  gap: 2rem;
  justify-content: start;
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
