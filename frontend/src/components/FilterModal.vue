<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  isOpen: Boolean,
  currentMin: Number,
  currentMax: Number
})

const emit = defineEmits(['close', 'apply'])

// Constants for slider
const MIN_LIMIT = 0
const MAX_LIMIT = 500000

// Local state
const minPrice = ref(props.currentMin ?? MIN_LIMIT)
const maxPrice = ref(props.currentMax ?? MAX_LIMIT)

// Sync with props
watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    minPrice.value = props.currentMin ?? MIN_LIMIT
    maxPrice.value = props.currentMax ?? MAX_LIMIT
  }
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
    max: maxPrice.value
  })
}
</script>

<template>
  <div v-if="isOpen" class="modal-wrapper">
    <!-- No overlay background -->
    <div class="modal-content">
      <div class="modal-header">
        <h3>가격 설정</h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>
      
      <div class="modal-body">
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
  width: 300px;
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
  margin-bottom: 20px;
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

.price-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
}

.slider-container {
  position: relative;
  height: 30px;
  margin-bottom: 20px;
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
</style>
