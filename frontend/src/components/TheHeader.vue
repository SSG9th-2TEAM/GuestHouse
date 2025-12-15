<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isMenuOpen = ref(false)
const isSearchExpanded = ref(false)
const isHostMode = ref(false)

const isMobile = () => window.innerWidth <= 768

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const toggleSearch = () => {
  if (isMobile()) {
    isSearchExpanded.value = !isSearchExpanded.value
  }
}

const handleClickOutside = (e) => {
  if (!e.target.closest('.right-menu')) {
    isMenuOpen.value = false
  }
}

const handleResize = () => {
  if (!isMobile()) {
    isSearchExpanded.value = false
  }
}

const toggleHostMode = () => {
  isHostMode.value = !isHostMode.value
  if (isHostMode.value) {
    router.push('/host')
  } else {
    router.push('/')
  }
  // Optional: keep menu open or close it. Usually navigating closes it.
  isMenuOpen.value = false 
}

const handleLogout = () => {
  // Mock logout
  alert('로그아웃 되었습니다.')
  isMenuOpen.value = false
  router.push('/')
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-content">
        <div class="header-top">
          <!-- Logo linked to home -->
          <router-link to="/">
            <img src="@/assets/logo.png" alt="Logo" class="logo" />
          </router-link>

          <div class="right-menu">
            <button class="hamburger-btn" @click.stop="toggleMenu" aria-label="메뉴">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
              </svg>
            </button>

            <!-- Enhanced Mobile Menu (Dropdown) -->
            <div class="mobile-menu-dropdown" :class="{ active: isMenuOpen }" @click.stop>
              <div class="menu-header">
                <span class="menu-title">메뉴</span>
                <button class="close-btn" @click="isMenuOpen = false">×</button>
              </div>

              <div class="host-toggle-card">
                <div class="toggle-info">
                  <div class="toggle-title">호스트 모드 전환</div>
                  <div class="toggle-status">현재: {{ isHostMode ? '호스트 모드' : '게스트 모드' }}</div>
                </div>
                <div class="toggle-switch" :class="{ active: isHostMode }" @click="toggleHostMode">
                  <div class="toggle-slider"></div>
                </div>
              </div>

              <ul class="menu-list">
                <li><router-link to="/profile" @click="isMenuOpen = false">프로필 정보</router-link></li>
                <li><router-link to="/reservations" @click="isMenuOpen = false">예약 내역</router-link></li>
                <li><router-link to="/reviews" @click="isMenuOpen = false">리뷰 내역</router-link></li>
                <li><router-link to="/wishlist" @click="isMenuOpen = false">위시리스트</router-link></li>
                <li><router-link to="/coupons" @click="isMenuOpen = false">쿠폰</router-link></li>
              </ul>

              <div class="menu-footer">
                <button class="logout-btn" @click="handleLogout">로그아웃</button>
              </div>
            </div>
          </div>
        </div>

        <div
          class="search-bar"
          :class="{ expanded: isSearchExpanded }"
          @click="toggleSearch"
        >
          <div class="search-item">
            <label>여행지</label>
            <input type="text" placeholder="어디로 갈까?">
          </div>

          <div class="search-divider"></div>

          <div class="search-item">
            <label>날짜</label>
            <input type="text" placeholder="날짜 선택">
          </div>

          <div class="search-divider"></div>

          <div class="search-item">
            <label>여행자</label>
            <input type="text" placeholder="게스트 추가">
          </div>

          <button class="search-btn" aria-label="검색">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: white;
  border-bottom: 1px solid #e8ecf0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 10px 16px;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  height: 40px;
  width: auto;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.8;
}

/* Search Bar Styles */
.search-bar {
  width: 100%;
  background: white;
  border: 1px solid #e0e6eb;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-bar:hover {
  border-color: #6DC3BB;
  box-shadow: 0 4px 12px rgba(109, 195, 187, 0.1);
}

.search-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 6px 8px;
  border-radius: 6px;
  min-width: 0;
}

.search-item:hover {
  background-color: #f0f7f6;
}

.search-item label {
  font-size: 10px;
  font-weight: 600;
  color: #8a92a0;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 2px;
  display: block;
  font-family: 'Poppins', sans-serif;
  white-space: nowrap;
}

.search-item input {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #1a1f36;
  outline: none;
  font-weight: 500;
  font-family: 'Noto Sans KR', sans-serif;
  width: 100%;
}

.search-item input::placeholder {
  color: #c5cdd4;
  font-weight: 400;
}

.search-divider {
  width: 1px;
  height: 20px;
  background-color: #e8ecf0;
  flex-shrink: 0;
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: #6DC3BB;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: white;
  transition: all 0.3s ease;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(109, 195, 187, 0.2);
}

.search-btn:hover {
  background-color: #5aaca3;
  box-shadow: 0 4px 16px rgba(109, 195, 187, 0.3);
  transform: scale(1.05);
}

.search-btn:active {
  transform: scale(0.95);
}

.search-btn svg {
  width: 16px;
  height: 16px;
}

/* Right Menu Area */
.right-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  position: relative; /* Anchor for dropdown */
}

.hamburger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #3d4452;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.hamburger-btn:hover {
  background-color: #f0f4f8;
  color: #1a1f36;
}

.hamburger-btn svg {
  width: 22px;
  height: 22px;
}

/* Enhanced Mobile Menu Dropdown */
.mobile-menu-dropdown {
  display: none;
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 300px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  padding: 24px;
  z-index: 1000;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
}

.mobile-menu-dropdown.active {
  display: block;
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.menu-title {
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

/* Host Toggle Card */
.host-toggle-card {
  background: #EFF6FF; /* Very light blue */
  padding: 16px 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.toggle-title {
  font-size: 14px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}

.toggle-status {
  font-size: 12px;
  color: #6B7280;
}

.toggle-switch {
  width: 44px;
  height: 24px;
  background: #D1D5DB;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background 0.3s;
}

.toggle-switch.active {
  background: #6DC3BB;
}

.toggle-slider {
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: left 0.3s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.toggle-switch.active .toggle-slider {
  left: 22px;
}

/* Menu List */
.menu-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
}

.menu-list li {
  margin-bottom: 12px;
}

.menu-list li:last-child {
  margin-bottom: 0;
}

.menu-list a {
  text-decoration: none;
  color: #374151;
  font-size: 15px;
  font-weight: 500;
  display: block;
  padding: 8px 0;
  transition: color 0.2s;
}

.menu-list a:hover {
  color: #6DC3BB;
  padding-left: 4px;
}

/* Menu Footer (Logout) */
.menu-footer {
  border-top: 1px solid #F3F4F6;
  padding-top: 20px;
}

.logout-btn {
  background: none;
  border: none;
  color: #EF4444; /* Red */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.logout-btn:hover {
  text-decoration: underline;
}

/* Desktop styles */
@media (min-width: 769px) {
  .header-container {
    padding: 16px 40px;
  }

  .header-content {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 32px;
  }

  .header-top {
    display: contents;
  }

  .logo {
    height: 48px;
    flex-shrink: 0;
    order: 1;
  }

  .search-bar {
    max-width: 850px;
    flex: 1;
    order: 2;
    border-radius: 40px;
    padding: 14px 28px;
    gap: 24px;
  }

  .search-item {
    padding: 10px 16px;
    min-width: 150px;
  }

  .search-item label {
    font-size: 12px;
    display: block !important;
  }

  .search-item input {
    font-size: 14px;
  }

  .search-divider {
    display: block !important;
    height: 32px;
  }

  .search-btn {
    width: 48px;
    height: 48px;
  }

  .search-btn svg {
    width: 20px;
    height: 20px;
  }

  .right-menu {
    flex-shrink: 0;
    order: 3;
  }

  .hamburger-btn {
    width: 44px;
    height: 44px;
  }

  .hamburger-btn svg {
    width: 24px;
    height: 24px;
  }
}

/* Mobile styles */
@media (max-width: 768px) {
  .app-header {
    overflow-x: hidden;
  }

  .header-container {
    padding: 8px 12px;
    max-width: 100%;
    overflow: hidden;
  }

  .header-content {
    gap: 10px;
    max-width: 100%;
  }

  .header-top {
    width: 100%;
  }

  .logo {
    height: 32px;
  }

  .right-menu {
    position: relative;
  }
  
  /* Mobile menu full width on very small screens if needed, 
     but 300px width is fine for most mobiles */
  .mobile-menu-dropdown {
    right: -8px; /* Alignment tweak */
    width: 280px;
  }

  .search-bar {
    width: 100%;
    max-width: 100%;
    padding: 8px 10px;
    gap: 8px;
  }

  .search-bar.expanded {
    flex-direction: column;
    gap: 0;
    padding: 12px;
  }

  .search-item {
    padding: 6px 4px;
    min-width: 0;
  }

  .search-item input {
    min-width: 0;
    width: 100%;
  }

  .search-bar:not(.expanded) .search-item label {
    display: none;
  }

  .search-bar:not(.expanded) .search-item input {
    font-size: 12px;
  }

  .search-bar.expanded .search-item {
    border-bottom: 1px solid #e8ecf0;
  }

  .search-bar.expanded .search-item:last-of-type {
    border-bottom: none;
  }

  .search-item:hover {
    background-color: transparent;
  }

  .search-bar:not(.expanded) .search-divider {
    display: none;
  }

  .search-divider {
    display: none;
  }

  .search-btn {
    width: 36px;
    height: 36px;
  }

  .search-bar.expanded .search-btn {
    width: 100%;
    border-radius: 8px;
    margin-top: 8px;
  }
}
</style>
