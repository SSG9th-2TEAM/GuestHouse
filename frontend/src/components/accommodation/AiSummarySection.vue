<script setup>
import { ref, onMounted } from 'vue'
import { fetchAiSummary } from '@/api/accommodation'

defineOptions({
  name: 'AiSummarySection'
})

onMounted(() => {
  // console.log('AiSummarySection Mounted!') // 디버깅 로그는 잠시 주석 처리
})

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

    <div v-if="isLoading" class="skeleton-loader">
      <div class="skeleton-line title"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line short"></div>
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
  margin: 1.5rem 0;
}

.ai-btn {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  border: none;
  border-radius: 16px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.ai-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4);
}

/* 카드 디자인 핵심 */
.summary-box {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  margin-top: 1rem;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 1.2rem;
  font-weight: 800;
  font-size: 1.15rem;
  color: #1f2937;
  border-bottom: 2px solid #f3f4f6;
  padding-bottom: 0.8rem;
}

.summary-text {
  white-space: pre-line;
  line-height: 1.8;
  color: #4b5563;
  font-size: 15px;
  margin: 0;
  letter-spacing: -0.01em;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 로딩 스켈레톤도 카드에 맞게 수정 */
.skeleton-loader {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  margin-top: 1rem;
}

.skeleton-line {
  height: 15px;
  background-color: #e5e7eb;
  border-radius: 6px;
  margin-bottom: 10px;
  animation: pulse 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
.skeleton-line.title {
  width: 40%;
  height: 20px;
  margin-bottom: 1rem;
}
.skeleton-line.short {
  width: 70%;
}
.skeleton-line:last-child {
  margin-bottom: 0;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: .5;
  }
}
</style>
