<script setup>
import { ref, onUnmounted } from 'vue'
import { fetchAiSummary } from '@/api/accommodation'

defineOptions({
  name: 'AiSummarySection'
})

const props = defineProps({
  accommodationId: {
    type: [Number, String],
    required: true
  }
})

const fullSummaryHtml = ref('')
const displayedSummary = ref('')
const isLoading = ref(false)
const isError = ref(false)
const isLoaded = ref(false)
let typeWriterInterval = null

const typeWriterEffect = (htmlContent) => {
  let i = 0;
  displayedSummary.value = '';

  if (typeWriterInterval) clearInterval(typeWriterInterval);

  typeWriterInterval = setInterval(() => {
    if (i >= htmlContent.length) {
      clearInterval(typeWriterInterval);
      return;
    }

    if (htmlContent[i] === '<') {
      const tagEndIndex = htmlContent.indexOf('>', i);
      if (tagEndIndex !== -1) {
        displayedSummary.value += htmlContent.substring(i, tagEndIndex + 1);
        i = tagEndIndex + 1;
        return;
      }
    }

    displayedSummary.value += htmlContent[i];
    i++;
  }, 20);
}

const buildSummaryHtml = (data) => {
  const { accommodationName, locationTag, keywords, moodDescription, tip, reviewCount } = data;

  const keywordsHtml = keywords.join(' ');
  const footerHtml = reviewCount > 0
    ? `<span class="footer-text">🔍 최근 <strong>${reviewCount}건</strong>의 실제 방문자 리뷰와 데이터를 기반으로 분석했습니다.</span>`
    : `<span class="footer-text">🔍 숙소 상세 정보를 기반으로 분석했습니다.</span>`;

  return `<strong>${accommodationName}</strong>은(는) <strong>${locationTag}</strong>에 위치한 매력적인 숙소입니다.<br><br>` +
         `🔑 <strong>핵심 키워드</strong>: ${keywordsHtml} #제주감성<br><br>` +
         `🏡 <strong>분위기 & 특징</strong><br>${moodDescription}<br><br>` +
         `💡 <strong>AI의 이용 꿀팁</strong><br>${tip}<br><br>` +
         footerHtml;
}

const loadSummary = async () => {
  if (isLoading.value || isLoaded.value) return

  isLoading.value = true
  isError.value = false

  try {
    const response = await fetchAiSummary(props.accommodationId)
    if (response.ok && response.data) {
      fullSummaryHtml.value = buildSummaryHtml(response.data)
      isLoaded.value = true
      typeWriterEffect(fullSummaryHtml.value)
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

onUnmounted(() => {
  if (typeWriterInterval) clearInterval(typeWriterInterval);
})
</script>

<template>
  <div class="ai-summary-section">
    <button
      v-if="!isLoaded && !isLoading && !isError"
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

    <div v-if="isError" class="error-box">
      <p>요약 정보를 불러오는데 실패했습니다.</p>
      <button class="retry-btn" @click="loadSummary">재시도</button>
    </div>

    <div v-if="isLoaded" class="summary-box">
      <div class="summary-header">
        <span class="ai-icon">✨</span>
        <span class="ai-title">AI 숙소 요약</span>
      </div>
      <p class="summary-text">
        <span v-html="displayedSummary"></span>
        <span class="cursor" v-if="displayedSummary.length < fullSummaryHtml.length">|</span>
      </p>
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

.summary-box {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  margin-top: 1rem;
  min-height: 200px;
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
  line-height: 1.8;
  color: #4b5563;
  font-size: 15px;
  margin: 0;
  letter-spacing: -0.01em;
}

/* Footer Text Style (Global style needed for v-html or deep selector) */
:deep(.footer-text) {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 20px;
  text-align: right;
  display: block;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: #6366f1;
  margin-left: 2px;
  vertical-align: middle;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

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
.skeleton-line.title { width: 40%; height: 20px; margin-bottom: 1rem; }
.skeleton-line.short { width: 70%; }
.skeleton-line:last-child { margin-bottom: 0; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}

.error-box {
  margin-top: 1rem;
  padding: 1.5rem;
  background: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 12px;
  text-align: center;
  color: #991b1b;
}

.retry-btn {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
.retry-btn:hover { background: #dc2626; }
</style>
