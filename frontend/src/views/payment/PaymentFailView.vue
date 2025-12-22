<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const errorCode = ref('')
const errorMessage = ref('')

onMounted(() => {
  errorCode.value = route.query.code || 'UNKNOWN'
  errorMessage.value = route.query.message || '결제가 취소되었거나 실패했습니다.'
})

const goBack = () => router.back()
const goHome = () => router.push('/')
</script>

<template>
  <div class="payment-fail-page">
    <div class="fail-container">
      <div class="error-icon">✕</div>
      <h1>결제에 실패했습니다</h1>
      <p class="error-code">에러 코드: {{ errorCode }}</p>
      <p class="error-message">{{ errorMessage }}</p>
      <div class="actions">
        <button class="btn secondary" @click="goBack">다시 시도</button>
        <button class="btn primary" @click="goHome">홈으로</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-fail-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.fail-container {
  text-align: center;
  padding: 2rem;
  max-width: 400px;
}

.error-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  color: white;
  border-radius: 50%;
  font-size: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

h1 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.error-code {
  color: #888;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.error-message {
  color: #e11d48;
  margin-bottom: 2rem;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
}

.btn.primary {
  background: #0064FF;
  color: white;
}

.btn.secondary {
  background: white;
  color: #333;
  border: 1px solid #ddd;
}
</style>
