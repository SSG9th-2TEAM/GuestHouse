// Auth API client for login, signup, token management
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// LocalStorage keys
const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_INFO_KEY = 'userInfo'

// Get tokens from localStorage
export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function getUserInfo() {
  const userInfo = localStorage.getItem(USER_INFO_KEY)
  return userInfo ? JSON.parse(userInfo) : null
}

// Save tokens to localStorage
export function saveTokens(accessToken, refreshToken) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

// Save user info to localStorage
export function saveUserInfo(userInfo) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

// Clear all auth data
export function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

// Check if user is logged in
export function isAuthenticated() {
  return !!getAccessToken()
}

// API request helper
async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  // Add Authorization header if access token exists
  const accessToken = getAccessToken()
  if (accessToken && !options.skipAuth) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers
    })

    const data = await response.json().catch(() => null)

    return {
      ok: response.ok,
      status: response.status,
      data
    }
  } catch (error) {
    console.error('API Request Error:', error)
    return {
      ok: false,
      status: 0,
      data: null,
      error: error.message
    }
  }
}

// 회원가입
export async function signup(signupData) {
  return apiRequest('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(signupData),
    skipAuth: true
  })
}

// 로그인
export async function login(email, password) {
  const response = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    skipAuth: true
  })

  // 로그인 성공 시 토큰 및 사용자 정보 저장
  if (response.ok && response.data) {
    saveTokens(response.data.accessToken, response.data.refreshToken)
    saveUserInfo({ email: email, role: response.data.role })
  }

  return response
}

// 이메일 중복 확인
export async function checkEmailDuplicate(email) {
  return apiRequest(`/api/auth/check-email?email=${encodeURIComponent(email)}`, {
    method: 'GET',
    skipAuth: true
  })
}

// 토큰 갱신
export async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return { ok: false, status: 401, data: null }
  }

  const response = await apiRequest('/api/auth/refresh', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${refreshToken}`
    },
    skipAuth: true
  })

  // 토큰 갱신 성공 시 새 토큰 저장
  if (response.ok && response.data) {
    saveTokens(response.data.accessToken, response.data.refreshToken)
  }

  return response
}

// 로그아웃
export function logout() {
  clearAuth()
}

// 인증이 필요한 API 요청
export async function authenticatedRequest(endpoint, options = {}) {
  let response = await apiRequest(endpoint, options)

  // 401 에러 시 토큰 갱신 시도
  if (response.status === 401) {
    const refreshResponse = await refreshAccessToken()

    if (refreshResponse.ok) {
      // 토큰 갱신 성공 시 원래 요청 재시도
      response = await apiRequest(endpoint, options)
    } else {
      // 토큰 갱신 실패 시 로그아웃
      clearAuth()
    }
  }

  return response
}

export default {
  signup,
  login,
  logout,
  checkEmailDuplicate,
  refreshAccessToken,
  authenticatedRequest,
  getAccessToken,
  getRefreshToken,
  getUserInfo,
  saveUserInfo,
  clearAuth,
  isAuthenticated
}
