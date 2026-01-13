<script setup>
import { ref } from 'vue'
import { fetchAiSummary } from '@/api/accommodation'

const props = defineProps({
  accommodationId: {
    type: [Number, String],
    required: true
  }
})

const summary = ref('')
const isLoading = ref(false)
const isError = ref(false)
const isLoaded = ref(false)

const loadSummary = async () => {
  if (isLoading.value || isLoaded.value) return

  isLoading.value = true
  isError.value = false

  try {
    const response = await fetchAiSummary(props.accommodationId)
    if (response.ok && response.data?.summary) {
      summary.value = response.data.summary
      isLoaded.value = true
    } else {
      throw new Error('Failed to load summary')
    }
  } catch (error) {
    console.error('AI Summary Error:', error)
    isError.value = true
    alert('요약을 불러올 수 없습니다. 잠시 후 다시 시도해주세요.')
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="ai-summary-section">
    <button
      v-if="!isLoaded && !isLoading"
      class="ai-btn"
      @click="loadSummary"
    >
      ✨ AI 숙소 요약 보기
    </button>

    <div v-if="isLoading" class="loading-box">
      <div class="spinner"></div>
      <span>AI가 숙소 리뷰와 정보를 분석 중입니다...</span>
    </div>

    <div v-if="isLoaded" class="summary-box">
      <div class="summary-header">
        <span class="ai-icon">✨</span>
        <span class="ai-title">AI 숙소 요약</span>
      </div>
      <p class="summary-text">{{ summary }}</p>
    </div>
  </div>
</template>

<style scoped>
.ai-summary-section {
  margin: 1rem 0;
}

.ai-btn {
  width: 100%;
  padding: 0.8rem;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: transform 0.2s, opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.ai-btn:hover {
  opacity: 0.95;
  transform: translateY(-1px);
}

.ai-btn:active {
  transform: translateY(0);
}

.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
  padding: 1.5rem;
  background: #f9fafb;
  border-radius: 12px;
  color: #6b7280;
  font-size: 0.95rem;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid #e5e7eb;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.summary-box {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1.2rem;
  animation: fadeIn 0.3s ease-out;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
  font-weight: 700;
  color: #4b5563;
}

.ai-icon {
  font-size: 1.2rem;
}

.summary-text {
  white-space: pre-line;
  line-height: 1.6;
  color: #374151;
  font-size: 0.95rem;
  margin: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
