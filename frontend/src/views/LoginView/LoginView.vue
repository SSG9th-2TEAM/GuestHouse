<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, saveUserInfo } from '@/api/authClient'

const router = useRouter()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const isLoading = ref(false)

// Modal State
const showModal = ref(false)
const modalMessage = ref('')
const modalType = ref('info')
const modalCallback = ref(null)

const openModal = (message, type = 'info', callback = null) => {
  modalMessage.value = message
  modalType.value = type
  modalCallback.value = callback
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (modalCallback.value) {
    modalCallback.value()
    modalCallback.value = null
  }
}

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const handleLogin = async () => {
  // 입력 검증
  if (!email.value || !password.value) {
    openModal('이메일과 비밀번호를 입력해주세요.', 'error')
    return
  }

  isLoading.value = true

  try {
    // 백엔드 API 호출
    const response = await login(email.value, password.value)

    if (response.ok && response.data) {
      // 로그인 성공
      // 사용자 정보 저장 (필요시)
      saveUserInfo({
        email: email.value
      })

      openModal('로그인 성공!', 'success', () => {
        router.push('/')
      })
    } else {
      // 로그인 실패
      openModal('로그인에 실패했습니다.\n이메일 또는 비밀번호를 확인해주세요.', 'error')
    }
  } catch (error) {
    console.error('로그인 에러:', error)
    openModal('로그인 중 오류가 발생했습니다.\n잠시 후 다시 시도해주세요.', 'error')
  } finally {
    isLoading.value = false
  }
}

const socialLogin = (provider) => {
  // 실제 구현 시 각 provider의 OAuth URL로 리다이렉트
  const urls = {
    Google: 'https://accounts.google.com/o/oauth2/v2/auth', // 예시 URL 추가해야됨
    Kakao: 'https://kauth.kakao.com/oauth/authorize', // 예시 URL
    Naver: 'https://nid.naver.com/oauth2.0/authorize' // 예시 URL
  }

  // 개발 중에는 모달로 표시
  openModal(`${provider} 로그인으로 이동합니다.`, 'info')

  // 실제 구현 시 주석 해제
  // window.location.href = urls[provider]
}

const goToSignup = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1>환영합니다</h1>
        <p>로그인하여 서비스를 이용하세요</p>
      </div>

      <div class="login-form">
        <div class="input-group">
          <label>이메일</label>
          <div class="input-wrapper">
            <input
                type="email"
                v-model="email"
                placeholder="이메일을 입력하세요"
                @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <div class="input-group">
          <label>비밀번호</label>
          <div class="input-wrapper">
            <input
                :type="showPassword ? 'text' : 'password'"
                v-model="password"
                placeholder="비밀번호를 입력하세요"
                @keyup.enter="handleLogin"
            />
            <button class="toggle-btn" @click="togglePassword">
              {{ showPassword ? '숨김' : '보기' }}
            </button>
          </div>
        </div>

        <button class="login-btn" @click="handleLogin" :disabled="isLoading">
          {{ isLoading ? '로그인 중...' : '로그인' }}
        </button>
      </div>

      <div class="divider">
        <span>또는</span>
      </div>

      <!-- <CHANGE> 소셜 로그인 버튼 디자인 변경 - 각각 고유한 배경색 적용 -->
      <div class="social-login">
        <button class="social-btn google-btn" @click="socialLogin('Google')">
          <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          <span>Google로 시작하기</span>
        </button>

        <button class="social-btn kakao-btn" @click="socialLogin('Kakao')">
          <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20">
            <path fill="#000000" d="M12 3C6.48 3 2 6.48 2 10.8c0 2.7 1.68 5.1 4.26 6.54-.18.66-.84 2.94-.96 3.36-.15.54.18.54.42.39.18-.12 2.94-1.98 3.42-2.34.54.06 1.08.12 1.86.12 5.52 0 10-3.48 10-7.8S17.52 3 12 3z"/>
          </svg>
          <span>Kakao로 시작하기</span>
        </button>

        <button class="social-btn naver-btn" @click="socialLogin('Naver')">
          <svg class="social-icon" viewBox="0 0 24 24" width="20" height="20">
            <path fill="#FFFFFF" d="M16.273 12.845L7.376 0H0v24h7.726V11.156L16.624 24H24V0h-7.727v12.845z"/>
          </svg>
          <span>Naver로 시작하기</span>
        </button>
      </div>

      <div class="signup-link">
        <span @click="goToSignup">회원가입</span>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-icon" :class="modalType">
          <span v-if="modalType === 'success'">✓</span>
          <span v-else-if="modalType === 'error'">!</span>
          <span v-else>i</span>
        </div>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  padding: 2rem;
}

.login-container {
  background: white;
  padding: 3rem 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  max-width: 400px;
  width: 100%;
}

.login-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.login-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 0.5rem;
}

.login-header p {
  font-size: 0.95rem;
  color: #888;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.input-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.input-wrapper {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 0 1rem;
  background: white;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: #00796b;
}

.input-wrapper input {
  flex: 1;
  border: none;
  padding: 1rem 0;
  font-size: 0.95rem;
  outline: none;
}

.toggle-btn {
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  opacity: 0.6;
}

.login-btn {
  width: 100%;
  padding: 1rem;
  background: #00796b;
  color: white;
  font-size: 1rem;
  font-weight: 700;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 0.5rem;
  transition: opacity 0.2s;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.login-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.7;
}

.divider {
  display: flex;
  align-items: center;
  margin: 2rem 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eee;
}

.divider span {
  padding: 0 1rem;
  font-size: 0.85rem;
  color: #888;
}

/* <CHANGE> 소셜 로그인 버튼 스타일 - 각 플랫폼별 고유 색상 적용 */
.social-login {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.social-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  width: 100%;
  padding: 1rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.social-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.social-btn:active {
  transform: translateY(0);
}

.social-icon {
  flex-shrink: 0;
}

/* Google 버튼 - 흰색 배경 */
.google-btn {
  background: #ffffff;
  color: #333;
  border: 1px solid #dadce0;
}

.google-btn:hover {
  background: #f8f9fa;
}

/* Kakao 버튼 - 노란색 배경 */
.kakao-btn {
  background: #fee500;
  color: #000000;
}

.kakao-btn:hover {
  background: #fdd835;
}

/* Naver 버튼 - 초록색 배경 */
.naver-btn {
  background: #03c75a;
  color: #ffffff;
}

.naver-btn:hover {
  background: #02b350;
}

.signup-link {
  text-align: center;
  margin-top: 2rem;
}

.signup-link span {
  color: #333;
  font-size: 0.95rem;
  cursor: pointer;
  text-decoration: underline;
}

.signup-link span:hover {
  color: #004d40;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 320px;
  width: 90%;
  text-align: center;
}

.modal-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 1.5rem;
  font-weight: bold;
}

.modal-icon.success {
  background: #a5d6a7;
  color: #2e7d32;
}

.modal-icon.error {
  background: #fee2e2;
  color: #dc2626;
}

.modal-icon.info {
  background: #e0f2fe;
  color: #0284c7;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 0.8rem;
  background: #00796b;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
</style>