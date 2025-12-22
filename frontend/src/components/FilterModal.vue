<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { fetchThemes } from '@/api/theme'

const props = defineProps({
  isOpen: Boolean,
  currentMin: Number,
  currentMax: Number,
  currentThemes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'apply'])

// Constants for slider
const MIN_LIMIT = 0
const MAX_LIMIT = 500000

// Local state
const minPrice = ref(props.currentMin ?? MIN_LIMIT)
const maxPrice = ref(props.currentMax ?? MAX_LIMIT)
const themes = ref([])
const selectedThemeIds = ref([...props.currentThemes])
const isLoadingThemes = ref(false)

// Sync with props
watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    minPrice.value = props.currentMin ?? MIN_LIMIT
    maxPrice.value = props.currentMax ?? MAX_LIMIT
    selectedThemeIds.value = [...props.currentThemes]

    if (!themes.value.length) {
      loadThemes()
    }
  }
})

const loadThemes = async () => {
  if (isLoadingThemes.value) return
  isLoadingThemes.value = true
  try {
    const { ok, data } = await fetchThemes()
    if (ok && Array.isArray(data)) {
      themes.value = data
    }
  } catch (e) {
    console.error('Failed to load themes', e)
  } finally {
    isLoadingThemes.value = false
  }
}

const toggleTheme = (id) => {
  if (selectedThemeIds.value.includes(id)) {
    selectedThemeIds.value = selectedThemeIds.value.filter(item => item !== id)
  } else {
    selectedThemeIds.value = [...selectedThemeIds.value, id]
  }
}

onMounted(() => {
  loadThemes()
})

// Ensure min <= max
const handleMinChange = () => {
  if (minPrice.value > maxPrice.value) {
    minPrice.value = maxPrice.value
  }
}

const handleMaxChange = () => {
  if (maxPrice.value < minPrice.value) {
    maxPrice.value = minPrice.value
  }
}

// Calculate percentages for slider track background
const minPercent = computed(() => ((minPrice.value - MIN_LIMIT) / (MAX_LIMIT - MIN_LIMIT)) * 100)
const maxPercent = computed(() => ((maxPrice.value - MIN_LIMIT) / (MAX_LIMIT - MIN_LIMIT)) * 100)

const applyFilter = () => {
  emit('apply', {
    min: minPrice.value,
    max: maxPrice.value,
    themeIds: [...selectedThemeIds.value]
  })
}
</script>

<template>
  <div v-if="isOpen" class="modal-wrapper">
    <!-- No overlay background -->
    <div class="modal-content">
      <div class="modal-header">
        <h3>필터</h3>
        <button class="close-btn" @click="$emit('close')">닫기</button>
      </div>
      
      <div class="modal-body">
        <div class="section-header">
          <span class="section-title">가격</span>
        </div>
        <div class="price-display">
          <span>{{ minPrice.toLocaleString() }}원</span>
          <span>~</span>
          <span>{{ maxPrice.toLocaleString() }}원</span>
        </div>

        <div class="slider-container">
          <div class="slider-track" :style="{ background: `linear-gradient(to right, #ddd ${minPercent}%, #222 ${minPercent}%, #222 ${maxPercent}%, #ddd ${maxPercent}%)` }"></div>
          <input 
            type="range" 
            :min="MIN_LIMIT" 
            :max="MAX_LIMIT" 
            step="10000" 
            v-model.number="minPrice" 
            @input="handleMinChange"
            class="range-input"
          />
          <input 
            type="range" 
            :min="MIN_LIMIT" 
            :max="MAX_LIMIT" 
            step="10000" 
            v-model.number="maxPrice" 
            @input="handleMaxChange"
            class="range-input"
          />
        </div>

        <div class="section-header space-top">
          <span class="section-title">테마</span>
          <span class="section-hint">복수 선택 가능</span>
        </div>
        <div class="theme-grid">
          <button
            v-for="theme in themes"
            :key="theme.id"
            type="button"
            class="theme-chip"
            :class="{ active: selectedThemeIds.includes(theme.id) }"
            @click="toggleTheme(theme.id)"
          >
            {{ theme.themeName }}
          </button>
          <div v-if="!themes.length && isLoadingThemes" class="theme-empty">테마를 불러오는 중...</div>
          <div v-else-if="!themes.length" class="theme-empty">테마 정보를 불러올 수 없습니다.</div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-apply" @click="applyFilter">적용</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-wrapper {
  position: fixed;
  top: 140px; /* Adjusted to clear header */
  left: 20px;
  z-index: 1000;
  /* Ensure it doesn't block interaction with the rest of the page outside the modal */
  pointer-events: none; 
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 320px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #eee;
  pointer-events: auto; /* Re-enable pointer events for the modal itself */
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.modal-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  padding: 4px;
  color: #999;
}

.modal-body {
  display: flex;
  flex-direction: column;
}

.price-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.slider-container {
  position: relative;
  height: 30px;
  margin-bottom: 16px;
}

.slider-track {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: #ddd;
}

.range-input {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 100%;
  height: 0;
  -webkit-appearance: none;
  pointer-events: none; /* Allow clicking through to track/other slider */
  background: transparent;
}

/* Helper for thumb styling */
.range-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  border: 2px solid #222;
  cursor: pointer;
  pointer-events: auto; /* Catch clicks on thumb */
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-top: -8px; /* Offset for track */
}

/* For Firefox */
.range-input::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  border: 2px solid #222;
  cursor: pointer;
  pointer-events: auto;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.modal-footer {
  text-align: right;
}

.btn-apply {
  background: #222;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
}

.btn-apply:hover {
  background: #000;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 8px;
}

.section-header:first-of-type {
  margin-top: 0;
}

.section-title {
  font-weight: 700;
  color: #222;
}

.section-hint {
  font-size: 0.85rem;
  color: #777;
}

.space-top {
  margin-top: 12px;
}

.theme-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 36px;
}

.theme-chip {
  border: 1px solid #ddd;
  border-radius: 18px;
  background: #fff;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.15s ease;
}

.theme-chip:hover {
  border-color: #222;
}

.theme-chip.active {
  background: #222;
  color: #fff;
  border-color: #222;
}

.theme-empty {
  color: #999;
  font-size: 0.9rem;
}
</style>
