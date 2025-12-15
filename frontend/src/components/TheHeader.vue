<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isMenuOpen = ref(false)
const isHostMode = ref(false)
const isLoggedIn = ref(true)

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const handleLogout = () => {
  isLoggedIn.value = false
  isMenuOpen.value = false
}

const handleLogin = () => {
  isLoggedIn.value = true
}

const openProfile = () => {
  isMenuOpen.value = false
  router.push('/profile')
}
</script>

<template>
  <header class="header">
    <div class="container header-content">
      <div class="logo" @click="router.push('/')" style="cursor: pointer;">
        <span class="logo-icon">🏠</span>
        <span class="logo-text">GuestHouse</span>
      </div>
      
      <div class="search-bar">
        <input type="text" placeholder="어디로 떠나시나요?" />
        <button class="search-btn">🔍</button>
      </div>

      <div class="profile" @click="toggleMenu">
        <div class="avatar">👤</div>
        
        <!-- Profile Menu Dropdown -->
        <div v-if="isMenuOpen" class="menu-dropdown" @click.stop>
          
          <!-- View 1: Main Menu -->
          <div class="menu-content">
            <div class="menu-header">
              <h3>메뉴</h3>
              <button class="close-btn" @click="toggleMenu">✕</button>
            </div>

            <!-- Logged In View -->
            <template v-if="isLoggedIn">
              <div class="host-mode-card">
                <div class="mode-info">
                  <span class="mode-title">호스트 모드 전환</span>
                  <span class="mode-status">현재: {{ isHostMode ? '호스트 모드' : '게스트 모드' }}</span>
                </div>
                <button class="mode-switch-btn" @click="isHostMode = !isHostMode; router.push(isHostMode ? '/host' : '/'); isMenuOpen = false;">
                  전환하기
                </button>
              </div>

              <ul class="menu-list">
                <li @click="openProfile">프로필 정보</li>
                <li @click="router.push('/reservations'); isMenuOpen = false">예약 내역</li>
                <li @click="router.push('/reviews'); isMenuOpen = false">리뷰 내역</li>
                <li @click="router.push('/wishlist'); isMenuOpen = false">위시리스트</li>
                <li @click="router.push('/coupons'); isMenuOpen = false">쿠폰</li>
              </ul>

              <div class="menu-footer">
                <span class="logout" @click="handleLogout">로그아웃</span>
              </div>
            </template>

            <!-- Logged Out View -->
            <template v-else>
              <div class="login-view">
                <p class="login-msg">로그인이 필요합니다.</p>
                <button class="login-btn-full" @click="router.push('/login'); isMenuOpen = false">로그인</button>
              </div>
            </template>
          </div>

        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 80px;
  background: var(--bg-white);
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  position: relative;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  font-size: 1.25rem;
  color: var(--text-main);
  flex-shrink: 0;
}

.search-bar {
  display: flex;
  align-items: center;
  background: #f3f4f6;
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  flex: 1;
  max-width: 400px;
  min-width: 120px;
  margin: 0 1rem;
  transition: box-shadow 0.2s;
}

.search-bar:focus-within {
  box-shadow: 0 0 0 2px var(--primary);
  background: white;
}

.search-bar input {
  border: none;
  background: transparent;
  flex: 1;
  outline: none;
  font-size: 0.95rem;
  min-width: 0;
}

.search-btn {
  background: none;
  font-size: 1.1rem;
}

.profile {
  position: relative;
  flex-shrink: 0;
}

.profile .avatar {
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  cursor: pointer;
}

/* Menu Dropdown Styles */
.menu-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 340px; /* Slightly wider for profile view */
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  padding: 1.5rem;
  z-index: 1000;
  border: 1px solid #f0f0f0;
  cursor: default;
  min-height: 400px; /* Keep height consistent */
}

/* Main Menu View */
.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.menu-header h3 {
  font-size: 1.1rem;
  font-weight: 700;
}

.close-btn {
  background: none;
  font-size: 1.2rem;
  color: #999;
  padding: 4px;
  cursor: pointer;
}

.host-mode-card {
  background: #F2F6FF;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.mode-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mode-title {
  font-weight: 700;
  font-size: 0.95rem;
}

.mode-status {
  font-size: 0.8rem;
  color: #666;
}

.mode-switch-btn {
  background: var(--bg-white);
  border: 1px solid #ddd;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #555;
  transition: all 0.2s;
}

.mode-switch-btn:hover {
  background: #f9f9f9;
  border-color: #ccc;
}

.menu-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  margin-bottom: 1.5rem;
}

.menu-list li {
  font-size: 0.95rem;
  color: #333;
  cursor: pointer;
  transition: color 0.2s;
}

.menu-list li:hover {
  color: var(--primary-hover);
}

.menu-footer {
  border-top: 1px solid #eee;
  padding-top: 1rem;
}

.logout {
  color: #FF4B4B;
  font-size: 0.95rem;
  cursor: pointer;
}

/* Login View Styles */
.login-view {
  text-align: center;
  padding: 2rem 0;
}

.login-msg {
  color: #666;
  margin-bottom: 1.5rem;
}

.login-btn-full {
  width: 100%;
  background-color: var(--primary);
  color: #004d40;
  font-weight: 700;
  padding: 0.8rem;
  border-radius: 8px;
  font-size: 1rem;
}

.login-btn-full:hover {
  opacity: 0.9;
}


</style>
