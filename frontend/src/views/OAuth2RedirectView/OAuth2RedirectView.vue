<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { saveTokens } from '@/api/authClient'

const router = useRouter()
const route = useRoute()

onMounted(async () => {
  // URL 파라미터에서 토큰 추출
  const accessToken = route.query.accessToken
  const refreshToken = route.query.refreshToken

  if (!accessToken || !refreshToken) {
    console.error('토큰이 없습니다.')
    router.push('/login')
    return
  }

  // 토큰 저장
  saveTokens(accessToken, refreshToken)

  // 현재 경로 확인하여 리다이렉트
  // /oauth2/redirect는 기존 사용자, /social-signup는 신규 사용자
  // OAuth2AuthenticationSuccessHandler에서 이미 분기 처리되어 리다이렉트됨

  // 메인 페이지로 이동 (기존 소셜 사용자)
  router.push('/')
})
</script>

<template>
  <div class="redirect-page">
    <div class="loading-container">
      <div class="spinner"></div>
      <p>로그인 처리 중...</p>
    </div>
  </div>
</template>

<style scoped>
.redirect-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.loading-container {
  text-align: center;
}

.spinner {
  width: 50px;
  height: 50px;
  margin: 0 auto 1rem;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #333;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-container p {
  font-size: 1rem;
  color: #666;
}
</style>
