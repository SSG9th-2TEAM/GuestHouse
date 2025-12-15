<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const userId = ref('')
const email = ref('')
const password = ref('')
const showPassword = ref(false)

// Modal State
const showModal = ref(false)
const modalMessage = ref('')
const modalType = ref('info') // 'info', 'success', 'error'
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

const handleLogin = () => {
  if (userId.value && email.value && password.value) {
    openModal('로그인 성공!', 'success', () => router.push('/'))
  } else {
    openModal('모든 필드를 입력해주세요.', 'error')
  }
}

const socialLogin = (provider) => {
  openModal(`${provider} 로그인은 현재 개발 중입니다.`, 'info')
}

const goToSignup = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Header -->
      <div class="login-header">
        <h1>환영합니다</h1>
        <p>로그인하여 서비스를 이용하세요</p>
      </div>

      <!-- Form -->
      <div class="login-form">
        <!-- User ID -->
        <div class="input-group">
          <label>아이디</label>
          <div class="input-wrapper">
            <input 
              type="text" 
              v-model="userId" 
              placeholder="아이디를 입력하세요"
            />
          </div>
        </div>

        <!-- Email -->
        <div class="input-group">
          <label>이메일</label>
          <div class="input-wrapper">
            <input 
              type="email" 
              v-model="email" 
              placeholder="이메일을 입력하세요"
            />
          </div>
        </div>

        <!-- Password -->
        <div class="input-group">
          <label>비밀번호</label>
          <div class="input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="password" 
              placeholder="비밀번호를 입력하세요"
            />
            <button class="toggle-btn" @click="togglePassword">
              {{ showPassword ? '숨김' : '보기' }}
            </button>
          </div>
        </div>

        <!-- Login Button -->
        <button class="login-btn" @click="handleLogin">로그인</button>
      </div>

      <!-- Divider -->
      <div class="divider">
        <span>또는</span>
      </div>

      <!-- Social Login -->
      <div class="social-login">
        <button class="social-btn" @click="socialLogin('Google')">
          <span class="social-icon google">●</span>
          Google로 로그인
        </button>
        <button class="social-btn" @click="socialLogin('Kakao')">
          <span class="social-icon kakao">●</span>
          카카오로 로그인
        </button>
        <button class="social-btn" @click="socialLogin('Naver')">
          <span class="social-icon naver">●</span>
          네이버로 로그인
        </button>
      </div>

      <!-- Signup Link -->
      <div class="signup-link">
        <span @click="goToSignup">회원가입</span>
      </div>
    </div>

    <!-- Modal -->
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

/* Form */
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
  border-color: var(--primary);
}

.input-icon {
  font-size: 1rem;
  margin-right: 0.75rem;
  opacity: 0.6;
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

/* Login Button */
.login-btn {
  width: 100%;
  padding: 1rem;
  background: var(--primary);
  color: #004d40;
  font-size: 1rem;
  font-weight: 700;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 0.5rem;
  transition: opacity 0.2s;
}

.login-btn:hover {
  opacity: 0.9;
}

/* Divider */
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

/* Social Login */
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
  padding: 0.9rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.social-btn:hover {
  background: #f9f9f9;
}

.social-icon {
  font-size: 0.8rem;
}

.social-icon.google {
  color: #4285f4;
}

.social-icon.kakao {
  color: #fee500;
}

.social-icon.naver {
  color: #03c75a;
}

/* Signup Link */
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

/* Modal */
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
  background: var(--primary);
  color: #004d40;
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
  background: var(--primary);
  color: #004d40;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
</style>
