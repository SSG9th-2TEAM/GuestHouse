<script setup>
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import FilterModal from '../../components/FilterModal.vue'
import { useRouter, useRoute } from 'vue-router'
import { searchList } from '@/api/list'
import { ref, computed, onMounted, watch } from 'vue'
import { useSearchStore } from '@/stores/search'

import { fetchWishlistIds, addWishlist, removeWishlist } from '@/api/wishlist'
import { isAuthenticated } from '@/api/authClient'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const items = ref([])
const wishlistIds = ref(new Set())
const page = ref(0)
const totalPages = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const PAGE_SIZE = 24

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
  const imageUrl = item.imageUrl || 'https://placehold.co/400x300'
  return { id, title, description, rating, location, price, imageUrl }
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

const loadList = async ({ themeIds = selectedThemeIds.value, keyword = searchStore.keyword, page: pageParam = 0, reset = false } = {}) => {
  if (isLoading.value || isLoadingMore.value) return
  if (reset) {
    isLoading.value = true
    page.value = 0
  } else {
    isLoadingMore.value = true
  }
  try {
    const response = await searchList({ themeIds, keyword, page: pageParam, size: PAGE_SIZE })
    if (response.ok) {
      const payload = response.data
      const list = Array.isArray(payload?.items) ? payload.items : []
      const normalized = list.map(normalizeItem)
      if (reset) {
        items.value = normalized
      } else {
        items.value = [...items.value, ...normalized]
      }
      const meta = payload?.page
      if (meta) {
        page.value = meta.number ?? pageParam
        totalPages.value = meta.totalPages ?? totalPages.value
      }
    } else {
      console.error('Failed to load list', response.status)
    }
  } catch (error) {
    console.error('Failed to load list', error)
  } finally {
    isLoading.value = false
    isLoadingMore.value = false
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
  loadList({ themeIds, reset: true })
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
  applyRouteKeyword()
  loadList({ reset: true })
})

watch(
  () => route.query.keyword,
  () => {
    applyRouteKeyword()
    loadList({ reset: true })
  }
)

const hasMore = computed(() => page.value + 1 < totalPages.value)

const loadMore = () => {
  if (!hasMore.value || isLoading.value || isLoadingMore.value) return
  const nextPage = page.value + 1
  loadList({ page: nextPage })
}
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

    <div class="list-footer" v-if="hasMore">
      <button class="load-more-btn" @click="loadMore" :disabled="isLoadingMore">
        {{ isLoadingMore ? '불러오는 중...' : '더 보기' }}
      </button>
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
  --card-width: 340px;
}

.header {
  margin-bottom: 0;
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
  justify-content: center;
}

.list-item {
  cursor: pointer;
  transition: transform 0.2s;
}

.list-item:hover {
  transform: translateY(-5px);
}

.list-footer {
  display: flex;
  justify-content: center;
  margin: 2rem 0 1rem;
}

.load-more-btn {
  padding: 10px 18px;
  border: 1px solid #ddd;
  border-radius: 24px;
  background: white;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
}

.load-more-btn:hover:not(:disabled) {
  background-color: #f5f5f5;
  transform: translateY(-1px);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
