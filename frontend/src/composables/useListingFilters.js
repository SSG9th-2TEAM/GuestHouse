import { useSearchStore } from '@/stores/search'

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

const parseGuestCount = (value) => {
  const parsed = parseNumberParam(value)
  if (!Number.isFinite(parsed)) return 0
  return Math.max(0, parsed)
}

export const useListingFilters = () => {
  const searchStore = useSearchStore()

  const applyRouteFilters = (query = {}) => {
    const minValue = parseNumberParam(query.min ?? query.minPrice)
    const maxValue = parseNumberParam(query.max ?? query.maxPrice)
    const themeIds = parseThemeIds(query.themeIds)
    const guestCount = parseGuestCount(query.guestCount)

    searchStore.setPriceRange(minValue, maxValue)
    searchStore.setThemeIds(themeIds)
    searchStore.setGuestCount(guestCount)
  }

  const buildFilterQuery = (keywordOverride) => {
    const query = {}

    if (searchStore.minPrice !== null) query.min = String(searchStore.minPrice)
    if (searchStore.maxPrice !== null) query.max = String(searchStore.maxPrice)
    if (searchStore.themeIds.length) query.themeIds = searchStore.themeIds.join(',')
    if (searchStore.guestCount > 0) query.guestCount = String(searchStore.guestCount)

    const keyword = String(keywordOverride ?? searchStore.keyword ?? '').trim()
    if (keyword) query.keyword = keyword

    return query
  }

  return {
    applyRouteFilters,
    buildFilterQuery
  }
}
